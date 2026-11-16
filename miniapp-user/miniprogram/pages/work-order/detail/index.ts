import Toast from '@vant/weapp/toast/toast';
import type {
  AcceptanceVO,
  AppointmentVO,
  QuoteItemVO,
  QuoteVO,
  ServiceRecordVO,
  TimelineEventVO,
  WorkOrderAllowedAction,
  WorkOrderDetailVO
} from '@fixmate/api-contract';
import {
  acceptQuote,
  cancelWorkOrder,
  getWorkOrderDetail,
  raiseAcceptanceIssue,
  rejectQuote,
  requestHumanHandoff,
  rescheduleAppointment,
  submitAcceptance,
  urge
} from '../../../services/workOrder';
import { ApiError } from '../../../utils/request';

const STATUS_TEXT: Record<string, string> = {
  WAITING_MANUAL_REVIEW: '等待人工处理',
  WAITING_CUSTOMER_VISIT: '待选上门时段',
  WAITING_ASSIGNMENT: '等待派单',
  WAITING_APPOINTMENT: '待确认预约',
  APPOINTED: '已预约',
  IN_INSPECTION: '上门检测中',
  WAITING_QUOTE: '等待报价',
  WAITING_QUOTE_CONFIRMATION: '待确认报价',
  IN_REPAIR: '维修中',
  PAUSED_FOR_REQUOTE: '维修暂停，待确认追加报价',
  WAITING_ACCEPTANCE: '待验收',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  CLOSED_UNRESOLVED: '已关闭'
};

const ACTION_TEXT: Record<string, string> = {
  ACCEPT_QUOTE: '接受报价',
  REJECT_QUOTE: '拒绝报价',
  RESCHEDULE: '申请改约',
  SELECT_PREFERRED_VISIT: '选择或调整返修时段',
  URGE: '催单',
  HUMAN_HANDOFF: '转人工',
  ACCEPTANCE: '确认验收',
  RAISE_ACCEPTANCE_ISSUE: '验收有问题',
  CANCEL_WORK_ORDER: '申请取消报修',
  CANCEL_WITH_VISIT_FEE: '申请取消报修'
};

const APPLIANCE_TEXT: Record<string, string> = {
  AIR_CONDITIONER: '空调',
  REFRIGERATOR: '冰箱',
  WASHING_MACHINE: '洗衣机',
  AIR_SOURCE_HEAT_PUMP: '空气能'
};

const BRAND_TEXT: Record<string, string> = {
  HAIER: '海尔',
  MIDEA: '美的',
  SIEMENS: '西门子'
};

/** 后端已下发中文文案；这里只防旧数据或未知事件把内部代码透给用户。 */
function looksInternal(text: string): boolean {
  return /[A-Z]{3,}_[A-Z_]/.test(text) || text.indexOf('→') >= 0 || /^[A-Z_]+$/.test(text);
}

function userTitle(raw: string): string {
  if (!raw || looksInternal(raw)) {
    return '进度已更新';
  }
  return raw;
}

function userDetail(raw: string): string {
  if (!raw || looksInternal(raw) || raw.indexOf('高风险') >= 0 || raw.indexOf('硬规则') >= 0) {
    return '';
  }
  return raw;
}

function pad(value: number): string {
  return value < 10 ? '0' + value : String(value);
}

function dateInput(value: Date): string {
  return value.getFullYear() + '-' + pad(value.getMonth() + 1) + '-' + pad(value.getDate());
}

function timeInput(value: Date): string {
  return pad(value.getHours()) + ':' + pad(value.getMinutes());
}

function offsetDate(date: string, time: string): Date {
  return new Date(date + 'T' + time + ':00+08:00');
}

function formatTime(value: string | Date | null | undefined): string {
  if (!value) {
    return '';
  }
  const date = value instanceof Date ? value : new Date(value);
  if (isNaN(date.getTime())) {
    return String(value);
  }
  return pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' +
    pad(date.getHours()) + ':' + pad(date.getMinutes());
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

interface ActionButton {
  action: WorkOrderAllowedAction;
  label: string;
  primary: boolean;
}

type PromptKind = 'reject' | 'handoff' | 'acceptance' | 'acceptanceIssue';

/** Vant 的 change 事件 detail 是字符串，miniprogram-api-typings 的 CustomEvent 只接受对象。 */
interface ValueEvent {
  detail: string;
}

interface TimelineRow {
  time: string;
  title: string;
  detail: string;
  current: boolean;
}

const QUOTE_EXPIRED_HINT = '上一份报价已过期，请等待维修人员重新报价。';

function quoteDecision(action: WorkOrderAllowedAction): boolean {
  return action === 'ACCEPT_QUOTE' || action === 'REJECT_QUOTE';
}

Page({
  _detail: null as WorkOrderDetailVO | null,
  _quoteExpiryTimer: undefined as number | undefined,
  _visible: false,
  data: {
    id: '',
    loading: true,
    loadError: '',
    acting: false,
    hasLoaded: false,

    workOrderNo: '',
    statusText: '',
    cancelled: false,
    awaitingVisit: false,
    applianceLabel: '',
    symptom: '',
    errorCodeText: '',
    occurredAtText: '',
    mediaFiles: [] as Array<{ id: string; url: string; loaded: boolean; error: string }>,
    mediaErrorDetail: '',
    contactName: '',
    contactPhone: '',
    addressText: '',
    cancellationFeeAmount: '',

    quote: null as QuoteVO | null,
    quoteVersionText: '',
    quoteEmptyHint: '',
    quoteTypeText: '',
    expiresText: '',
    changeReason: '',
    items: [] as QuoteItemVO[],
    history: [] as Array<{ quoteNo: string; typeText: string; status: string; total: string }>,

    appointment: null as AppointmentVO | null,
    appointmentText: '',
    technicianName: '',
    serviceRecords: [] as Array<{ time: string; technician: string; summary: string }>,
    acceptance: null as AcceptanceVO | null,
    acceptanceText: '',

    timeline: [] as TimelineRow[],
    supportUpdates: [] as Array<{key:string;time:string;title:string;detail:string}>,
    actions: [] as ActionButton[],

    acceptVisible: false,
    promptShow: false,
    promptKind: '' as '' | PromptKind,
    promptTitle: '',
    promptPlaceholder: '',
    promptMaxlength: 200,

    rescheduleVisible: false,
    rescheduleDate: '',
    rescheduleStart: '',
    rescheduleEnd: '',
    rescheduleReason: '',
    rescheduleError: ''
  },

  onLoad(options: Record<string, string | undefined>) {
    const id = (options.id || '').trim();
    if (!id) {
      this.setData({ loading: false, loadError: '缺少工单编号' });
      return;
    }
    // 契约要求 IdString 为正整数串；编译模式里带了尖括号或空格时在这里就报出来
    if (!/^[1-9][0-9]{0,18}$/.test(id)) {
      this.setData({
        loading: false,
        loadError: '工单编号格式不正确，请检查编译模式的启动参数（应为 id=数字）'
      });
      return;
    }
    this.setData({ id });
    this.load();
  },

  /** 返回本页时静默重取一次：详情数据可能已被其他端改变，只靠 onLoad 会看到旧状态。 */
  onShow() {
    this._visible = true;
    this.checkQuoteExpiry();
    if (this.data.hasLoaded && this.data.id) {
      this.refresh();
    }
  },

  onHide() {
    this._visible = false;
    this.clearQuoteExpiryTimer();
  },

  onUnload() {
    this.onHide();
  },

  onPullDownRefresh() {
    this.refresh().then(() => wx.stopPullDownRefresh());
  },

  async load() {
    this.setData({ loading: true, loadError: '' });
    try {
      const detail = await getWorkOrderDetail(this.data.id);
      this.applyDetail(detail);
    } catch (error) {
      this.setData({ loading: false, loadError: messageOf(error) });
    }
  },

  /** 不切加载态的后台刷新，避免返回本页时闪骨架。 */
  async refresh() {
    try {
      const detail = await getWorkOrderDetail(this.data.id);
      this.applyDetail(detail);
    } catch (error) {
      if (!this.data.hasLoaded) {
        this.setData({ loadError: messageOf(error) });
      }
    }
  },

  applyDetail(detail: WorkOrderDetailVO) {
    const quoteChanged = this.data.quote?.id !== detail.currentQuote?.id;
    this._detail = detail;
    const appointment = detail.appointment?.status==='CANCELLED'?null:detail.appointment;
    const support = detail.timeline.filter(event=>event.eventType==='URGE_UPDATE'||event.eventType==='HUMAN_HANDOFF_UPDATE');
    this.setData({
      loading: false,
      hasLoaded: true,
      workOrderNo: detail.workOrderNo,
      statusText: appointment?.status==='RESCHEDULE_REQUESTED'?'改约待确认':STATUS_TEXT[detail.status] || '处理中',
      cancelled: detail.status==='CANCELLED',
      awaitingVisit: detail.status==='WAITING_CUSTOMER_VISIT',
      applianceLabel: [
        APPLIANCE_TEXT[detail.applianceType] || '设备',
        BRAND_TEXT[detail.brandCode || ''] || '',
        detail.modelCode || ''
      ].filter((item) => !!item).join(' · '),
      symptom: detail.symptomDescription,
      errorCodeText: detail.errorCodeText || '',
      occurredAtText: detail.occurredAt ? formatTime(detail.occurredAt) : '',
      mediaFiles: (detail.mediaFiles || [])
        .filter((file) => !!file.url)
        .map((file) => ({ id: file.id, url: file.url, loaded: false, error: '' })),
      mediaErrorDetail: '',
      contactName: detail.contactName,
      contactPhone: detail.contactPhone,
      addressText: detail.addressText,
      cancellationFeeAmount: detail.cancellationFeeAmount ?? '',
      ...this.quoteFields(detail),
      ...(quoteChanged ? this.closeQuoteDialogs() : {}),
      appointment: appointment,
      appointmentText: appointment
        ? formatTime(appointment.scheduledStartAt) + ' 至 ' + formatTime(appointment.scheduledEndAt)
        : '',
      technicianName: detail.technicianName || '',
      serviceRecords: detail.serviceRecords.map((item: ServiceRecordVO) => ({
        time: formatTime(item.inspectedAt),
        technician: item.technicianName,
        summary: item.summary
      })),
      acceptance: detail.acceptance,
      acceptanceText: detail.acceptance && detail.acceptance.acceptedAt
        ? '验收时间 ' + formatTime(detail.acceptance.acceptedAt)
        : '',
      timeline: this.buildTimeline(detail.timeline.filter(event=>!support.includes(event))),
      supportUpdates: support.reverse().map((event,index)=>({
        key:event.eventType+'-'+index,time:formatTime(event.occurredAt),title:event.title,detail:event.detail||''
      }))
    });
    this.checkQuoteExpiry();
  },

  quoteFields(detail: WorkOrderDetailVO) {
    const quote = detail.currentQuote;
    return {
      quote,
      quoteVersionText: quote ? '第 ' + quote.versionNo + ' 版' : '',
      quoteEmptyHint: !quote && detail.quoteHistory[detail.quoteHistory.length - 1]?.status === 'EXPIRED'
        ? QUOTE_EXPIRED_HINT : '',
      quoteTypeText: quote ? (quote.quoteType === 'ADDITIONAL' ? '本次新增' : '本次报价') : '',
      expiresText: quote?.expiresAt ? formatTime(quote.expiresAt) : '',
      changeReason: quote?.changeReason || '',
      items: quote?.items || [],
      history: detail.quoteHistory.map((item: QuoteVO) => ({
        quoteNo: item.quoteNo,
        typeText: item.quoteType === 'ADDITIONAL' ? '追加报价' : '初始报价',
        status: ({ ACCEPTED: '已接受', REJECTED: '已拒绝', WITHDRAWN: '已撤回', EXPIRED: '已过期', SUBMITTED: '待确认' } as Record<string, string>)[item.status] || item.status,
        total: item.payableTotalAfterAcceptance
      })),
      actions: this.buildActions(detail.allowedActions)
    };
  },

  closeQuoteDialogs() {
    return this.data.promptKind === 'reject'
      ? { acceptVisible: false, promptShow: false }
      : { acceptVisible: false };
  },

  clearQuoteExpiryTimer() {
    if (this._quoteExpiryTimer !== undefined) clearTimeout(this._quoteExpiryTimer);
    this._quoteExpiryTimer = undefined;
  },

  checkQuoteExpiry() {
    this.clearQuoteExpiryTimer();
    const detail = this._detail;
    const quote = detail?.currentQuote;
    if (!detail || !quote?.expiresAt) return;
    const remaining = new Date(quote.expiresAt).getTime() - Date.now();
    if (!Number.isFinite(remaining)) return;
    if (remaining > 0) {
      if (this._visible) {
        this._quoteExpiryTimer = setTimeout(() => this.checkQuoteExpiry(), Math.min(remaining, 2147483647));
      }
      return;
    }
    this.setData({
      ...this.quoteFields({
        ...detail,
        currentQuote: null,
        quoteHistory: [...detail.quoteHistory, { ...quote, status: 'EXPIRED' }],
        allowedActions: detail.allowedActions.filter(action => !quoteDecision(action))
      }),
      ...this.closeQuoteDialogs()
    });
  },

  buildTimeline(events: TimelineEventVO[]): TimelineRow[] {
    return events
      .slice()
      .reverse()
      .map((event: TimelineEventVO, index: number) => ({
        time: formatTime(event.occurredAt),
        title: userTitle(event.title || ''),
        detail: userDetail(event.detail || ''),
        current: index === 0
      }));
  },

  /** 后端授权决定可用动作，页面到期时可提前收起报价操作。 */
  buildActions(allowed: WorkOrderAllowedAction[]): ActionButton[] {
    const order = ['ACCEPT_QUOTE', 'ACCEPTANCE', 'RAISE_ACCEPTANCE_ISSUE', 'REJECT_QUOTE', 'SELECT_PREFERRED_VISIT', 'RESCHEDULE', 'URGE', 'HUMAN_HANDOFF',
      'CANCEL_WORK_ORDER', 'CANCEL_WITH_VISIT_FEE'];
    const buttons: ActionButton[] = [];
    order.forEach((action) => {
      if (allowed.indexOf(action as WorkOrderAllowedAction) < 0) {
        return;
      }
      buttons.push({
        action: action as WorkOrderAllowedAction,
        label: ACTION_TEXT[action] || action,
        primary: action === 'ACCEPT_QUOTE' || action === 'ACCEPTANCE' || action === 'SELECT_PREFERRED_VISIT'
      });
    });
    return buttons;
  },

  onSelectContinuationVisit() {
    if (this.data.awaitingVisit&&!this.data.acting) {
      wx.navigateTo({ url: '/pages/repair/visit-slot/index?id='+encodeURIComponent(this.data.id)+'&continuation=1' });
    }
  },

  openReworkVisit() {
    wx.navigateTo({ url: '/pages/repair/visit-slot/index?id='+encodeURIComponent(this.data.id)+'&rework=1' });
  },

  onAction(e: WechatMiniprogram.BaseEvent) {
    if (this.data.acting) {
      return;
    }
    const dataset = e.currentTarget.dataset as { action: WorkOrderAllowedAction };
    if (quoteDecision(dataset.action)) this.checkQuoteExpiry();
    switch (dataset.action) {
      case 'SELECT_PREFERRED_VISIT':
        this.openReworkVisit();
        break;
      case 'CANCEL_WORK_ORDER':
      case 'CANCEL_WITH_VISIT_FEE':
        this.confirmCancellation(dataset.action==='CANCEL_WITH_VISIT_FEE');
        break;
      case 'ACCEPT_QUOTE':
        this.confirmAccept();
        break;
      case 'REJECT_QUOTE':
        this.confirmReject();
        break;
      case 'RESCHEDULE':
        this.confirmReschedule();
        break;
      case 'URGE':
        this.confirmUrge();
        break;
      case 'HUMAN_HANDOFF':
        this.confirmHandoff();
        break;
      case 'ACCEPTANCE':
        this.confirmAcceptance();
        break;
      case 'RAISE_ACCEPTANCE_ISSUE':
        this.openPrompt('acceptanceIssue', '验收有问题', '请说明验收时发现的问题（2–1000 字）', 1000);
        break;
      default:
        Toast('该操作暂未开放');
    }
  },

  onMediaLoad(e: WechatMiniprogram.BaseEvent) {
    const index = Number((e.currentTarget.dataset as { index: number }).index || 0);
    this.setData({ ['mediaFiles[' + index + '].loaded']: true });
  },

  onMediaError(e: WechatMiniprogram.CustomEvent<{ errMsg: string }>) {
    const index = Number((e.currentTarget.dataset as { index: number }).index || 0);
    const raw = (e.detail && e.detail.errMsg) || '未知错误';
    // 原始信息里带完整签名地址（含 OSSAccessKeyId），展示前去掉链接本身
    const reason = raw.replace(/https?:\/\/\S+/g, '（图片地址）').trim();
    console.warn('work order media load failed', index, reason);
    this.setData({
      ['mediaFiles[' + index + '].error']: 'failed',
      mediaErrorDetail: '第 ' + (index + 1) + ' 张加载失败：' + reason
    });
  },

  onPreviewImage(e: WechatMiniprogram.BaseEvent) {
    const index = Number((e.currentTarget.dataset as { index: number }).index || 0);
    const urls = this.data.mediaFiles.map((file) => file.url);
    if (!urls.length) {
      return;
    }
    wx.previewImage({ current: urls[index] || urls[0], urls });
  },

  /** 接受报价：van-dialog 展示版本与三项金额后再确认。 */
  confirmAccept() {
    if (!this.data.quote) {
      return;
    }
    this.setData({ acceptVisible: true });
  },

  onAcceptCancel() {
    this.setData({ acceptVisible: false });
  },

  onAcceptConfirm() {
    if (this.data.acting || !this.data.acceptVisible) {
      return;
    }
    this.checkQuoteExpiry();
    const quote = this.data.quote;
    this.setData({ acceptVisible: false });
    if (!quote) {
      return;
    }
    this.run(() => acceptQuote(quote.id, quote.versionNo), '已接受报价');
  },

  confirmReject() {
    if (!this.data.quote) {
      return;
    }
    this.openPrompt('reject', '拒绝报价', '请填写拒绝原因（2–200 字）', 200);
  },

  confirmReschedule() {
    const appointment = this.data.appointment;
    const now = Date.now();
    let base = appointment ? new Date(appointment.scheduledStartAt) : new Date(now + 24 * 3600 * 1000);
    let end = appointment ? new Date(appointment.scheduledEndAt) : new Date(base.getTime() + 2 * 3600 * 1000);
    if (isNaN(base.getTime()) || base.getTime() < now) {
      // 已过期的预约不能作为默认值，否则用户一打开就注定校验失败
      base = new Date(now + 24 * 3600 * 1000);
      base.setHours(9, 0, 0, 0);
      end = new Date(base.getTime() + 2 * 3600 * 1000);
    }
    this.setData({
      rescheduleVisible: true,
      rescheduleDate: dateInput(base),
      rescheduleStart: timeInput(base),
      rescheduleEnd: timeInput(end),
      rescheduleReason: '',
      rescheduleError: ''
    });
  },

  onRescheduleCancel() {
    this.setData({ rescheduleVisible: false, rescheduleError: '' });
  },

  onRescheduleDateChange(e: WechatMiniprogram.CustomEvent<{ value: string }>) {
    this.setData({ rescheduleDate: e.detail.value, rescheduleError: '' });
  },

  onRescheduleStartChange(e: WechatMiniprogram.CustomEvent<{ value: string }>) {
    this.setData({ rescheduleStart: e.detail.value, rescheduleError: '' });
  },

  onRescheduleEndChange(e: WechatMiniprogram.CustomEvent<{ value: string }>) {
    this.setData({ rescheduleEnd: e.detail.value, rescheduleError: '' });
  },

  onRescheduleReason(e: ValueEvent) {
    this.setData({ rescheduleReason: e.detail, rescheduleError: '' });
  },

  onRescheduleConfirm() {
    if (this.data.acting) {
      return;
    }
    const reason = this.data.rescheduleReason.trim();
    if (reason.length < 2 || reason.length > 200) {
      this.setData({ rescheduleError: '请填写 2–200 字的改约原因' });
      return;
    }
    const startAt = offsetDate(this.data.rescheduleDate, this.data.rescheduleStart);
    const endAt = offsetDate(this.data.rescheduleDate, this.data.rescheduleEnd);
    if (isNaN(startAt.getTime()) || isNaN(endAt.getTime())) {
      this.setData({ rescheduleError: '请选择有效的日期与时间' });
      return;
    }
    if (endAt.getTime() <= startAt.getTime()) {
      this.setData({ rescheduleError: '结束时间必须晚于开始时间' });
      return;
    }
    if (startAt.getTime() < Date.now()) {
      this.setData({ rescheduleError: '开始时间不能早于当前时间' });
      return;
    }
    this.setData({ rescheduleVisible: false, rescheduleError: '' });
    this.run(() => rescheduleAppointment(this.data.id, {
      expectedWorkOrderVersion: this.versionOf(),
      startAt,
      endAt,
      reason
    }), '已提交改约申请');
  },

  async confirmUrge() {
    if (this.data.acting) {
      return;
    }
    this.setData({ acting: true });
    const confirmed = await new Promise<boolean>((resolve) => {
      wx.showModal({
        title: '催单',
        content: '确认催单吗？',
        confirmText: '确认催单',
        success: (result) => resolve(result.confirm),
        fail: () => resolve(false)
      });
    });
    if (!confirmed) {
      this.setData({ acting: false });
      return;
    }
    await this.run(() => urge(this.data.id, {
      expectedWorkOrderVersion: this.versionOf()
    }), { message: '已催单，请耐心等待', duration: 2000 });
  },

  async confirmCancellation(withVisitFee:boolean) {
    const version=this.versionOf();
    const fee=withVisitFee?this.data.cancellationFeeAmount:undefined;
    if(withVisitFee&&!fee){Toast('请刷新工单后核对上门费');return;}
    this.setData({ acting:true });
    const confirmed=await new Promise<boolean>(resolve=>wx.showModal({
      title:'取消本次报修？',
      content:withVisitFee?'确认后工单结束，仅需支付上门费 ¥'+fee+'。':'确认后本次报修和预约将结束，无需支付费用。',
      confirmText:'确认取消',
      success:result=>resolve(result.confirm),
      fail:()=>resolve(false)
    }));
    if(!confirmed){this.setData({acting:false});return;}
    await this.run(()=>cancelWorkOrder(this.data.id,version,fee),'已取消报修');
  },

  confirmHandoff() {
    this.openPrompt('handoff', '转人工', '请说明需要人工协助的原因（2–500 字）', 500);
  },

  confirmAcceptance() {
    this.openPrompt('acceptance', '确认验收', '可填写验收备注（选填）', 500);
  },

  openPrompt(kind: PromptKind, title: string, placeholder: string, maxlength: number) {
    this.setData({
      promptShow: true,
      promptKind: kind,
      promptTitle: title,
      promptPlaceholder: placeholder,
      promptMaxlength: maxlength
    });
  },

  onPromptCancel() {
    this.setData({ promptShow: false });
  },

  onPromptConfirm(e: WechatMiniprogram.CustomEvent<{ value: string }>) {
    if(this.data.acting)return;
    const value = (e.detail.value || '').trim();
    const kind = this.data.promptKind;
    if (kind === 'reject' && !this.data.promptShow) return;
    if (kind === 'reject') this.checkQuoteExpiry();
    const quote = this.data.quote;
    if(kind==='acceptanceIssue'&&(value.length<2||value.length>1000)){
      Toast('请填写 2–1000 字的问题说明');
      return;
    }
    this.setData({ promptShow: false });

    if(kind==='acceptanceIssue'){
      if(!this.data.actions.some(action=>action.action==='RAISE_ACCEPTANCE_ISSUE'))return;
      return this.run(()=>raiseAcceptanceIssue(this.data.id,{
        expectedWorkOrderVersion:this.versionOf(),reason:value
      }), '已提交验收问题，请选择返修时段');
    }
    if (kind === 'reject') {
      if (!quote) {
        return;
      }
      if (value.length < 2 || value.length > 200) {
        Toast('原因需 2–200 字');
        return;
      }
      this.run(() => rejectQuote(quote.id, { expectedQuoteVersion: quote.versionNo, reason: value }),
        '已拒绝报价');
      return;
    }
    if (kind === 'handoff') {
      if (value.length < 2) {
        Toast('请填写至少 2 个字');
        return;
      }
      this.run(() => requestHumanHandoff(this.data.id, {
        expectedWorkOrderVersion: this.versionOf(),
        reason: value
      }), '已转人工，请等待联系');
      return;
    }
    if (kind === 'acceptance') {
      this.run(() => submitAcceptance(this.data.id, {
        expectedWorkOrderVersion: this.versionOf(),
        accepted: true,
        note: value || undefined
      }), '已确认验收');
    }
  },

  versionOf(): number {
    return this._detail?.version ?? 0;
  },

  async run(action: () => Promise<unknown>, success: string | { message: string; duration: number }) {
    this.setData({ acting: true });
    try {
      await action();
      Toast.success(success);
      await this.refresh();
    } catch (error) {
      const message = messageOf(error);
      Toast.fail(message);
      if (error instanceof ApiError && (error.status === 409 || error.code === 'QUOTE_NOT_CURRENT')) {
        this.load();
      }
    } finally {
      this.setData({ acting: false });
    }
  }
});
