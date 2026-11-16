import Toast from '@vant/weapp/toast/toast';
import type {
  AddressVO,
  BrandOption,
  EnumOption,
  RepairDraftUpsertDTO,
  RepairFormMetaVO,
  RepairRequestVO,
  SafetyAnswersDTO,
  SafetyBlockVO,
  SelectableModelOption
} from '@fixmate/api-contract';
import { getRepairFormMeta } from '../../../services/meta';
import { createAddress, listAddresses } from '../../../services/address';
import { createDraft, getRepairRequest, submitRepairRequest, cancelRepairRequest, updateDraft } from '../../../services/repair';
import { uploadRepairImage } from '../../../services/file';
import { getWorkOrderDetail, requestHumanHandoff } from '../../../services/workOrder';
import { ApiError } from '../../../utils/request';
import { newIdempotencyKey, newRequestId } from '../../../utils/id';

import { getCurrentProfile } from '../../../services/profile';
import { syncTab } from '../../../utils/navigation';
import { DRAFT_KEY, PENDING_SUBMIT_KEY, adoptLegacyRecovery, customerId, pendingSubmit, readRecovery, writeRecovery, removeRecovery, takeRepairIntent } from '../../../utils/repairRecovery';
const SUBMIT_RECOVERY_WINDOW_MS = 30 * 60 * 1000;
interface PendingSubmit { draftId: string; version: number; key: string; at: number }
let pendingSubmitAction: PendingSubmit | null = null;
const MAX_MEDIA = 6;
const SYMPTOM_MIN = 10;
const SYMPTOM_MAX = 1000;

type AnswerKey =
  | 'smokeOrBurningSmell'
  | 'electricLeakage'
  | 'gasSmell'
  | 'suspectedRefrigerantLeak'
  | 'severeWaterIngress';

type PickerKind = 'type' | 'brand' | 'model' | 'address';

/** Vant 的 change 事件 detail 是字符串，miniprogram-api-typings 的 CustomEvent 只接受对象。 */
interface ValueEvent {
  detail: string;
  currentTarget: { dataset: Record<string, string> };
}

interface SafetyRow {
  key: AnswerKey;
  label: string;
  value: '' | 'yes' | 'no';
}

interface MediaItem {
  taskId: string;
  id: string;
  preview: string;
  uploading: boolean;
  error?: string;
  unresolved?: boolean;
}

interface UploaderFile {
  url: string;
  isImage: boolean;
  deletable: boolean;
  uploading: boolean;
}

const SAFETY_ORDER: Array<{ key: AnswerKey; label: string }> = [
  { key: 'smokeOrBurningSmell', label: '冒烟、焦味或明火' },
  { key: 'electricLeakage', label: '漏电或麻手' },
  { key: 'gasSmell', label: '燃气异味' },
  { key: 'suspectedRefrigerantLeak', label: '疑似制冷剂泄漏' },
  { key: 'severeWaterIngress', label: '严重进水' }
];

const MANUAL_MODEL_TEXT = '找不到？手动填写型号';

function buildSafetyRows(meta: RepairFormMetaVO | null): SafetyRow[] {
  const labels: Record<string, string> = {};
  if (meta && meta.safetyQuestions) {
    meta.safetyQuestions.forEach((item) => {
      labels[item.code] = item.label;
    });
  }
  return SAFETY_ORDER.map((item) => ({
    key: item.key,
    label: labels[item.key] || item.label,
    value: '' as const
  }));
}

function pad(value: number): string {
  return value < 10 ? '0' + value : String(value);
}

function datePartsOf(value: string | Date): { date: string; time: string } {
  const date = value instanceof Date ? value : new Date(value);
  if (isNaN(date.getTime())) {
    return { date: '', time: '' };
  }
  return {
    date: date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()),
    time: pad(date.getHours()) + ':' + pad(date.getMinutes())
  };
}

function nowTimeString(): string {
  const now = new Date();
  return pad(now.getHours()) + ':' + pad(now.getMinutes());
}

/** 用户填写的是北京时间；序列化后以 UTC 带偏移的形式发送。 */
function toDate(date: string, time: string): Date {
  return new Date(date + 'T' + (time || '00:00') + ':00+08:00');
}

function messageOf(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '操作失败，请稍后重试';
}

function chooseImages(count: number): Promise<WechatMiniprogram.MediaFile[]> {
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => resolve(res.tempFiles),
      fail: (err) => reject(err)
    });
  });
}

let cachedDraft: RepairRequestVO | null = null;

Page({
  data: {
    loading: true,
    loadError: '',
    submitting: false,
    saving: false,
    savedHint: '',
    submitError: '',
    pendingSubmit: false,
    pendingSubmitVersion: 0,

    typeOptions: [] as EnumOption[],
    applianceType: '',
    applianceTypeText: '',

    brandOptions: [] as BrandOption[],
    brandId: '',
    brandText: '',

    modelOptions: [] as SelectableModelOption[],
    modelId: '',
    modelTextDisplay: '',
    useModelText: false,
    modelText: '',

    serialNumber: '',
    symptom: '',
    errorCode: '',
    occurredDate: '',
    occurredTime: '',

    media: [] as MediaItem[],
    uploaderFiles: [] as UploaderFile[],
    safetyRows: buildSafetyRows(null),

    addressOptions: [] as AddressVO[],
    addressId: '',
    addressDisplay: '',
    addressFormOpen: false,
    addressSaving: false,
    addressError: '',
    addressForm: {
      contactName: '',
      contactPhone: '',
      province: '',
      city: '',
      district: '',
      detail: ''
    },

    pickerShow: false,
    pickerKind: '' as '' | PickerKind,
    pickerTitle: '',
    pickerColumns: [] as Array<{ text: string }>,
    pickerIndex: 0,

    draftId: '',
    version: 0,
    draftPrompt: false,
    canCancelDraft: false,
    canCancelSubmitted: false,
    cancelVisible: false,
    cancelling: false,

    blocked: null as SafetyBlockVO | null,
    submitted: null as RepairRequestVO | null,

    errors: {} as Record<string, string>
  },

  _ready: false,
  _owner: '',

  onLoad() {
    const pending = pendingSubmit();
    if (pending && typeof pending.draftId === 'string' && typeof pending.key === 'string'
        && typeof pending.version === 'number' && typeof pending.at === 'number') {
      pendingSubmitAction = pending;
      this.setData({ draftId: pending.draftId, pendingSubmit: true, pendingSubmitVersion: pending.version });
    }
    this.bootstrap();
  },

  updateTab() { syncTab(this, 0, !!this.data.blocked); },

  async onShow() {
    this.updateTab();
    if (!this._ready) return;
    if (this._owner !== customerId()) { wx.reLaunch({ url: '/pages/repair/new/index' }); return; }
    await this.applyRepairIntent();
  },

  async applyRepairIntent() {
    const intent = takeRepairIntent();
    if (!intent) return;
    const pending = pendingSubmit();
    if (pending) {
      pendingSubmitAction = pending;
      this.setData({ draftId: pending.draftId, pendingSubmit: true, pendingSubmitVersion: pending.version,
        blocked: null, submitted: null, draftPrompt: false });
      this.updateTab(); return;
    }
    if (intent.mode !== 'draft' || (intent.id === this.data.draftId && !this.data.submitted)) return;
    if (this.data.saving || this.data.submitting || this.data.media.some(item => item.uploading || item.unresolved)) {
      Toast('请先处理当前报修中的上传或保存'); return;
    }
    if (!this.data.submitted && !this.data.blocked && (this.data.symptom || this.data.applianceType)) {
      const confirmed = await new Promise<boolean>(resolve => wx.showModal({ title: '继续另一份草稿？',
        content: '当前未保存的填写内容会被替换，请先确认已保存。', success: res => resolve(res.confirm), fail: () => resolve(false) }));
      if (!confirmed) return;
    }
    try {
      const draft = await getRepairRequest(intent.id);
      if ((draft.allowedActions || []).indexOf('EDIT_DRAFT') < 0) { Toast('该记录已不能编辑，请刷新报修记录'); return; }
      cachedDraft = draft;
      this.setData({ blocked: null, submitted: null, pendingSubmit: false, draftPrompt: false,
        safetyRows: buildSafetyRows(null), errors: {}, submitError: '', savedHint: '' });
      writeRecovery(DRAFT_KEY, draft.id);
      this.onContinueDraft();
      this.updateTab();
    } catch (error) { Toast.fail(messageOf(error)); }
  },

  async bootstrap() {
    this.setData({ loading: true, loadError: '' });
    try {
      await getCurrentProfile();
      this._owner = customerId();
      await adoptLegacyRecovery(getRepairRequest);
      const pending = pendingSubmit();
      if (pending) { pendingSubmitAction = pending; this.setData({ draftId: pending.draftId, pendingSubmit: true, pendingSubmitVersion: pending.version }); }
      const meta: RepairFormMetaVO = await getRepairFormMeta();
      const addresses: AddressVO[] = await listAddresses();
      const first = addresses.length ? addresses[0] : null;
      this.setData({
        loading: false,
        typeOptions: meta.applianceTypes,
        brandOptions: meta.brands,
        modelOptions: meta.models,
        safetyRows: buildSafetyRows(meta),
        addressOptions: addresses,
        addressId: first ? first.id : '',
        addressDisplay: first ? first.province + first.city + first.district + first.detail : ''
      });
      this._ready = true;
      await this.restoreDraft();
      await this.applyRepairIntent();
      this.updateTab();
    } catch (error) {
      this.setData({ loading: false, loadError: messageOf(error) });
    }
  },

  async restoreDraft() {
    if (this.data.pendingSubmit) return;
    const draftId = (readRecovery<string>(DRAFT_KEY)) || '';
    if (!draftId) {
      return;
    }
    try {
      const draft = await getRepairRequest(draftId);
      if (draft.status === 'DRAFT') {
        cachedDraft = draft;
        this.setData({ draftPrompt: true });
      } else {
        removeRecovery(DRAFT_KEY);
      }
    } catch (error) {
      if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
        removeRecovery(DRAFT_KEY);
      } else {
        this.setData({ submitError: '草稿暂时无法读取，内容仍保留，请重试' });
      }
    }
  },

  onContinueDraft() {
    const draft = cachedDraft;
    this.setData({ draftPrompt: false });
    if (!draft) {
      return;
    }
    const answers = draft.safetyAnswers as SafetyAnswersDTO | null;
    const address = this.data.addressOptions.filter((item) => item.id === draft.addressId)[0];
    const occurred = draft.occurredAt ? datePartsOf(draft.occurredAt) : { date: '', time: '' };
    this.setData({
      draftId: draft.id,
      version: draft.version,
      canCancelDraft: (draft.allowedActions || []).indexOf('CANCEL') >= 0,
      applianceType: draft.applianceType || '',
      applianceTypeText: this.labelOfType(draft.applianceType || ''),
      brandId: draft.brandId || '',
      brandText: this.labelOfBrand(draft.brandId || ''),
      modelId: draft.modelId || '',
      modelText: draft.modelText || '',
      useModelText: !draft.modelId && !!draft.modelText,
      serialNumber: draft.serialNumber || '',
      symptom: draft.symptomDescription || '',
      errorCode: draft.errorCodeText || '',
      occurredDate: occurred.date,
      occurredTime: occurred.time,
      media: (draft.mediaFiles || []).map((file) => ({
        taskId: 'saved-' + file.id,
        id: file.id,
        preview: file.url,
        uploading: false
      })),
      safetyRows: this.data.safetyRows.map((row) => {
        if (!answers) {
          return row;
        }
        const value = answers[row.key];
        return { ...row, value: value === true ? 'yes' : value === false ? 'no' : '' };
      }),
      addressId: draft.addressId || '',
      addressDisplay: address ? address.province + address.city + address.district + address.detail : ''
    }, () => {
      this.refreshModelOptions();
      this.syncUploaderFiles();
    });
  },

  onDiscardDraft() {
    removeRecovery(DRAFT_KEY);
    cachedDraft = null;
    this.setData({ draftPrompt: false, draftId: '', version: 0 });
  },

  labelOfType(code: string): string {
    const option = this.data.typeOptions.filter((item) => item.code === code)[0];
    return option ? option.label : '';
  },

  labelOfBrand(id: string): string {
    const option = this.data.brandOptions.filter((item) => item.id === id)[0];
    return option ? option.name : '';
  },

  modelOptions(): SelectableModelOption[] {
    const brandId = this.data.brandId;
    const applianceType = this.data.applianceType;
    return this.data.modelOptions.filter((item) => {
      if (brandId && item.brandId !== brandId) {
        return false;
      }
      if (applianceType && item.applianceType !== applianceType) {
        return false;
      }
      return true;
    });
  },

  refreshModelOptions() {
    this.setData({ modelTextDisplay: this.currentModelLabel() });
  },

  currentModelLabel(): string {
    if (this.data.useModelText) {
      return this.data.modelText || '';
    }
    const current = this.modelOptions().filter((item) => item.id === this.data.modelId)[0];
    return current ? (current.displayName || current.modelCode) : '';
  },

  /** 选择器统一走一个 van-popup + van-picker，避免四个弹层各自维护状态。 */
  onOpenPicker(e: WechatMiniprogram.BaseEvent) {
    const kind = (e.currentTarget.dataset as { kind: PickerKind }).kind;
    let columns: Array<{ text: string }> = [];
    let index = 0;
    let title = '';
    if (kind === 'type') {
      title = '设备类型';
      columns = this.data.typeOptions.map((item) => ({ text: item.label }));
      index = Math.max(0, this.data.typeOptions.findIndex((item) => item.code === this.data.applianceType));
    } else if (kind === 'brand') {
      title = '品牌';
      columns = this.data.brandOptions.map((item) => ({ text: item.name }));
      index = Math.max(0, this.data.brandOptions.findIndex((item) => item.id === this.data.brandId));
    } else if (kind === 'model') {
      title = '型号';
      const options = this.modelOptions();
      columns = options.map((item) => ({ text: item.displayName || item.modelCode }));
      columns.push({ text: MANUAL_MODEL_TEXT });
      index = Math.max(0, options.findIndex((item) => item.id === this.data.modelId));
    } else {
      title = '服务地址';
      columns = this.data.addressOptions.map((item) => ({
        text: item.contactName + ' · ' + item.province + item.city + item.district
      }));
      index = Math.max(0, this.data.addressOptions.findIndex((item) => item.id === this.data.addressId));
    }
    this.setData({ pickerShow: true, pickerKind: kind, pickerTitle: title, pickerColumns: columns, pickerIndex: index });
  },

  onPickerClose() {
    this.setData({ pickerShow: false });
  },

  onPickerConfirm(e: WechatMiniprogram.CustomEvent<{ index: number }>) {
    const index = e.detail.index;
    const kind = this.data.pickerKind;
    this.setData({ pickerShow: false });
    if (kind === 'type') {
      const option = this.data.typeOptions[index];
      if (!option) {
        return;
      }
      this.setData({
        applianceType: option.code,
        applianceTypeText: option.label,
        modelId: '',
        modelTextDisplay: '',
        'errors.applianceType': ''
      }, () => this.refreshModelOptions());
      return;
    }
    if (kind === 'brand') {
      const option = this.data.brandOptions[index];
      if (!option) {
        return;
      }
      this.setData({
        brandId: option.id,
        brandText: option.name,
        modelId: '',
        modelTextDisplay: '',
        'errors.brand': ''
      }, () => this.refreshModelOptions());
      return;
    }
    if (kind === 'model') {
      const options = this.modelOptions();
      if (index >= options.length) {
        this.setData({ useModelText: true, modelId: '', modelTextDisplay: '', 'errors.model': '' });
        return;
      }
      const option = options[index];
      this.setData({
        useModelText: false,
        modelId: option.id,
        modelTextDisplay: option.displayName || option.modelCode,
        'errors.model': ''
      });
      return;
    }
    const address = this.data.addressOptions[index];
    if (!address) {
      return;
    }
    this.setData({
      addressId: address.id,
      addressDisplay: address.province + address.city + address.district + address.detail,
      'errors.address': ''
    });
  },

  onModelText(e: ValueEvent) {
    this.setData({ modelText: e.detail, modelTextDisplay: e.detail, 'errors.model': '' });
  },

  onSerialNumber(e: ValueEvent) {
    this.setData({ serialNumber: e.detail });
  },

  onSymptom(e: ValueEvent) {
    this.setData({ symptom: e.detail, 'errors.symptom': '' });
  },

  onErrorCode(e: ValueEvent) {
    this.setData({ errorCode: e.detail });
  },

  onDateChange(e: WechatMiniprogram.CustomEvent<{ value: string }>) {
    const date = e.detail.value;
    const time = this.data.occurredTime || nowTimeString();
    this.setData({ occurredDate: date, occurredTime: time });
  },

  onTimeChange(e: WechatMiniprogram.CustomEvent<{ value: string }>) {
    this.setData({ occurredTime: e.detail.value });
  },

  onAnswer(e: WechatMiniprogram.BaseEvent) {
    const dataset = e.currentTarget.dataset as { key: AnswerKey; value: 'yes' | 'no' };
    this.setData({
      safetyRows: this.data.safetyRows.map((row) =>
        row.key === dataset.key ? { ...row, value: dataset.value } : row),
      'errors.safety': ''
    });
  },

  onOpenAddressForm() {
    this.setData({ addressFormOpen: true, addressError: '' });
  },

  onCloseAddressForm() {
    this.setData({ addressFormOpen: false, addressError: '' });
  },

  onAddressField(e: WechatMiniprogram.Input | WechatMiniprogram.TextareaInput) {
    const { field } = e.currentTarget.dataset;
    this.setData({ ['addressForm.' + field]: e.detail.value });
  },

  async onSaveAddress() {
    if (this.data.addressSaving) {
      return;
    }
    const form = this.data.addressForm;
    if (!form.contactName.trim() || !form.province.trim() || !form.city.trim()
      || !form.district.trim() || !form.detail.trim()) {
      this.setData({ addressError: '请填写完整的联系人与地址' });
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(form.contactPhone.trim())) {
      this.setData({ addressError: '请填写 11 位手机号' });
      return;
    }
    this.setData({ addressSaving: true, addressError: '' });
    try {
      const created = await createAddress({
        contactName: form.contactName.trim(),
        contactPhone: form.contactPhone.trim(),
        province: form.province.trim(),
        city: form.city.trim(),
        district: form.district.trim(),
        detail: form.detail.trim()
      });
      const addresses = this.data.addressOptions.concat(created);
      this.setData({
        addressSaving: false,
        addressFormOpen: false,
        addressOptions: addresses,
        addressId: created.id,
        addressDisplay: created.province + created.city + created.district + created.detail,
        addressForm: { contactName: '', contactPhone: '', province: '', city: '', district: '', detail: '' },
        'errors.address': ''
      });
      Toast.success('地址已添加');
    } catch (error) {
      this.setData({ addressSaving: false, addressError: messageOf(error) });
    }
  },

  /** van-uploader 读完文件后由我们上传到后端，再把服务端 id 写回列表。 */
  async onAfterRead(e: WechatMiniprogram.CustomEvent<{ file: { url: string }; index: number }>) {
    if (this.data.pendingSubmit || this.data.submitting || this.data.saving) return;
    if (this.data.media.length >= MAX_MEDIA) {
      Toast('最多上传 ' + MAX_MEDIA + ' 张照片');
      return;
    }
    const url = e.detail.file.url;
    const taskId = newRequestId();
    const media = this.data.media.slice();
    media.push({ taskId, id: '', preview: url, uploading: false });
    this.setData({ media }, () => this.syncUploaderFiles());
    await this.uploadMediaTask(taskId);
  },

  async onRetryMedia(e: WechatMiniprogram.BaseEvent) {
    const taskId = (e.currentTarget.dataset as { task: string }).task;
    if (this.data.pendingSubmit || this.data.submitting || this.data.saving) return;
    await this.uploadMediaTask(taskId);
  },

  async uploadMediaTask(taskId: string) {
    const task = this.data.media.find((item) => item.taskId === taskId);
    if (!task || task.uploading || task.id) return;
    this.setData({ media: this.data.media.map((item) => item.taskId === taskId
      ? { ...item, uploading: true, error: '' } : item) }, () => this.syncUploaderFiles());
    try {
      const asset = await uploadRepairImage(task.preview);
      const next = this.data.media.map((item) => item.taskId === taskId
        ? { ...item, id: asset.id, uploading: false, unresolved: false, error: '' } : item);
      this.setData({ media: next }, () => this.syncUploaderFiles());
    } catch (error) {
      const unresolved = !(error instanceof ApiError) || error.status === 0 || error.status >= 500
        || error.code === 'DUPLICATE_REQUEST';
      const next = this.data.media.map((item) => item.taskId === taskId
        ? { ...item, uploading: false, unresolved, error: messageOf(error) } : item);
      this.setData({ media: next }, () => this.syncUploaderFiles());
      Toast.fail(messageOf(error));
    }
  },

  onDeleteMedia(e: WechatMiniprogram.CustomEvent<{ index: number }>) {
    if (this.data.pendingSubmit || this.data.submitting || this.data.saving) return;
    const media = this.data.media.slice();
    if (media[e.detail.index]?.uploading || media[e.detail.index]?.unresolved) {
      Toast('这张照片结果尚未确认，请重试原上传任务');
      return;
    }
    media.splice(e.detail.index, 1);
    this.setData({ media }, () => this.syncUploaderFiles());
  },

  syncUploaderFiles() {
    this.setData({
      uploaderFiles: this.data.media.map((item) => ({
        url: item.preview,
        isImage: true,
        deletable: !item.uploading && !item.unresolved,
        uploading: item.uploading
      }))
    });
  },

  buildPayload(): RepairDraftUpsertDTO {
    // 契约里五项答案要么全给、要么整体为 null；不能把"未回答"伪装成"否"
    const allAnswered = this.data.safetyRows.every((row) => row.value !== '');
    const answers: SafetyAnswersDTO | null = allAnswered ? {
      smokeOrBurningSmell: this.data.safetyRows[0].value === 'yes',
      electricLeakage: this.data.safetyRows[1].value === 'yes',
      gasSmell: this.data.safetyRows[2].value === 'yes',
      suspectedRefrigerantLeak: this.data.safetyRows[3].value === 'yes',
      severeWaterIngress: this.data.safetyRows[4].value === 'yes'
    } : null;
    return {
      applianceType: (this.data.applianceType || null) as RepairDraftUpsertDTO['applianceType'],
      brandId: this.data.brandId || null,
      modelId: this.data.useModelText ? null : this.data.modelId || null,
      modelText: this.data.useModelText ? this.data.modelText || null : null,
      serialNumber: this.data.serialNumber.trim() ? this.data.serialNumber : null,
      symptomDescription: this.data.symptom.trim() ? this.data.symptom : null,
      errorCodeText: this.data.errorCode.trim() ? this.data.errorCode : null,
      occurredAt: this.data.occurredDate ? toDate(this.data.occurredDate, this.data.occurredTime) : null,
      mediaFileIds: this.data.media.filter((item) => item.id).map((item) => item.id),
      safetyAnswers: answers,
      addressId: this.data.addressId || null
    };
  },

  validateForSubmit(): boolean {
    const errors: Record<string, string> = {};
    if (!this.data.applianceType) {
      errors.applianceType = '请选择设备类型';
    }
    if (!this.data.brandId) {
      errors.brand = '请选择品牌';
    }
    if (this.data.useModelText && !this.data.modelText.trim()) {
      errors.model = '请填写型号';
    } else if (!this.data.useModelText && !this.data.modelId) {
      errors.model = '请选择型号，或填写型号文本';
    }
    const symptom = this.data.symptom.trim();
    if (symptom.length < SYMPTOM_MIN) {
      errors.symptom = '请至少填写 ' + SYMPTOM_MIN + ' 个字';
    } else if (symptom.length > SYMPTOM_MAX) {
      errors.symptom = '最多 ' + SYMPTOM_MAX + ' 个字';
    }
    if (this.data.safetyRows.some((row) => row.value === '')) {
      errors.safety = '五项安全问题必须全部回答';
    }
    if (!this.data.addressId) {
      errors.address = '请选择服务地址';
    }
    this.setData({ errors });
    return Object.keys(errors).length === 0;
  },

  async saveDraft(silent: boolean): Promise<boolean> {
    if (this._owner && this._owner !== customerId()) { Toast('登录身份已变化，请重新进入报修'); return false; }
    if (this.data.media.some((item) => item.uploading || !item.id)) {
      Toast('请先完成照片上传，或移除明确失败的照片');
      return false;
    }
    if (!this.data.applianceType && !this.data.symptom) {
      if (!silent) {
        Toast('还没有可保存的内容');
      }
      return false;
    }
    this.setData({ saving: true, submitError: '' });
    try {
      const payload = this.buildPayload();
      let draft: RepairRequestVO;
      if (this.data.draftId) {
        draft = await updateDraft(this.data.draftId, { ...payload, expectedVersion: this.data.version });
      } else {
        draft = await createDraft(payload);
      }
      writeRecovery(DRAFT_KEY, draft.id);
      this.setData({
        saving: false,
        draftId: draft.id,
        version: draft.version,
        canCancelDraft: (draft.allowedActions || []).indexOf('CANCEL') >= 0,
        savedHint: '已保存 ' + nowTimeString(),
        errors: {}
      });
      return true;
    } catch (error) {
      this.setData({ saving: false, submitError: messageOf(error) });
      return false;
    }
  },

  async onSaveDraft() {
    if (this.data.saving || this.data.submitting) return;
    if (this.data.pendingSubmit) {
      Toast('上次提交结果未知，请先点击「重试提交」');
      return;
    }
    await this.saveDraft(false);
  },

  async onSubmit() {
    if (this.data.submitting || this.data.saving) {
      return;
    }
    this.setData({ submitError: '' });
    if (this.data.pendingSubmit) {
      // 结果未知时优先重放原提交：再次保存会因草稿已提交而 409，反而拿不到重放结果
      await this.submitSavedDraft(this.data.pendingSubmitVersion);
      return;
    }
    if (!this.validateForSubmit()) {
      Toast('请先完成必填项');
      return;
    }
    this.setData({ submitting: true });
    // 提交前总是把当前表单（含新上传的照片）落库：草稿已存在时也要覆盖，
    // 否则上次保存之后改的内容不会进入本次提交。
    const saved = await this.saveDraft(true);
    if (!saved) {
      this.setData({ submitting: false });
      return;
    }
    const submitVersion = this.data.version;
    pendingSubmitAction = { draftId: this.data.draftId, version: submitVersion,
      key: newIdempotencyKey(), at: Date.now() };
    writeRecovery(PENDING_SUBMIT_KEY, pendingSubmitAction);
    this.setData({ pendingSubmit: true, pendingSubmitVersion: submitVersion });
    await this.submitSavedDraft(submitVersion);
  },

  async submitSavedDraft(version: number) {
    if (this._owner && this._owner !== customerId()) { this.setData({submitting:false,submitError:'登录身份已变化，请重新进入报修'}); return; }
    const action = pendingSubmitAction;
    if (!action || action.draftId !== this.data.draftId || action.version !== version) {
      this.setData({ submitError: '无法确认原提交，请保留报修编号并联系人工核对' });
      return;
    }
    if (Date.now() - action.at > SUBMIT_RECOVERY_WINDOW_MS) {
      this.setData({ submitError: '提交结果仍未确认，请联系人工核对；系统不会重新创建报修' });
      return;
    }
    this.setData({ submitting: true, submitError: '' });
    try {
      const result = await submitRepairRequest(action.draftId, action.version, action.key);
      this.clearPendingSubmit();
      removeRecovery(DRAFT_KEY);
      this.setData({
        submitting: false,
        submitted: result,
        pendingSubmit: false,
        pendingSubmitVersion: 0,
        canCancelDraft: false,
        canCancelSubmitted: (result.allowedActions || []).indexOf('CANCEL') >= 0,
        savedHint: ''
      });
      wx.pageScrollTo({ scrollTop: 0 });
    } catch (error) {
      this.setData({ submitting: false });
      if (error instanceof ApiError && error.code === 'HARD_RULE_BLOCKED') {
        this.clearPendingSubmit();
        removeRecovery(DRAFT_KEY);
        this.setData({
          blocked: error.data as SafetyBlockVO,
          pendingSubmit: false,
          pendingSubmitVersion: 0
        });
        this.updateTab();
        wx.pageScrollTo({ scrollTop: 0 });
        return;
      }
      if (!(error instanceof ApiError) || error.code === 'NETWORK_ERROR' || error.status >= 500
          || error.code === 'DUPLICATE_REQUEST') {
        this.setData({
          pendingSubmit: true,
          pendingSubmitVersion: version,
          submitError: '正在确认上次提交，重试只恢复原内容；确认结果前暂不能编辑'
        });
        Toast.fail('提交结果未知，请重试');
        return;
      }
      this.clearPendingSubmit();
      this.setData({
        pendingSubmit: false,
        pendingSubmitVersion: 0,
        submitError: messageOf(error)
      });
      Toast.fail(messageOf(error));
    }
  },

  clearPendingSubmit() {
    pendingSubmitAction = null;
    removeRecovery(PENDING_SUBMIT_KEY);
  },

  onViewWorkOrder() {
    const blocked = this.data.blocked;
    const submitted = this.data.submitted;
    const workOrderId = blocked ? blocked.emergencyWorkOrderId : submitted ? submitted.workOrderId : '';
    if (!workOrderId) {
      return;
    }
    wx.navigateTo({ url: '/pages/work-order/detail/index?id=' + workOrderId });
  },

  /** 阻断态的「联系人工」真正落一条转人工记录，而不是只弹提示。 */
  async onContactHuman() {
    const blocked = this.data.blocked;
    if (!blocked) {
      return;
    }
    try {
      const detail = await getWorkOrderDetail(blocked.emergencyWorkOrderId);
      await requestHumanHandoff(blocked.emergencyWorkOrderId, {
        expectedWorkOrderVersion: detail.version,
        reason: '高风险阻断后申请人工联系'
      });
      Toast.success('已转人工，请等待联系');
    } catch (error) {
      Toast.fail(messageOf(error));
    }
  },

  /** 仅在服务端 allowedActions 含 CANCEL 时才会显示入口；二次确认后调用取消端点。 */
  onCancelRepair() {
    if (this.data.cancelling) {
      return;
    }
    this.setData({ cancelVisible: true });
  },

  onCancelCancel() {
    this.setData({ cancelVisible: false });
  },

  async onCancelConfirm() {
    const submitted = this.data.submitted;
    const id = submitted ? submitted.id : this.data.draftId;
    const expectedVersion = submitted ? submitted.version : this.data.version;
    if (!id) {
      return;
    }
    this.setData({ cancelVisible: false, cancelling: true });
    try {
      await cancelRepairRequest(id, { expectedVersion });
      removeRecovery(DRAFT_KEY);
      Toast.success('已取消报修');
      setTimeout(() => wx.reLaunch({ url: '/pages/repair/new/index' }), 600);
    } catch (error) {
      this.setData({ cancelling: false });
      Toast.fail(messageOf(error));
    }
  },

  onExitSafety() {
    if (!this.data.blocked) return;
    // 只退出展示；已落库紧急工单及安全事件不变，不重交原申请。
    wx.reLaunch({url:'/pages/repair/new/index'});
  },

  onStartOver() {
    removeRecovery(DRAFT_KEY);
    wx.reLaunch({ url: '/pages/repair/new/index' });
  }
});
