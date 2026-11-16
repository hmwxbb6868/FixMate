/** Vant 的 change 事件 detail 是字符串，miniprogram-api-typings 的 CustomEvent 只接受对象。 */
interface ValueEvent {
  detail: string;
  currentTarget: { dataset: Record<string, string> };
}

Component({
  options: {
    styleIsolation: 'apply-shared'
  },
  properties: {
    show: { type: Boolean, value: false },
    title: { type: String, value: '' },
    placeholder: { type: String, value: '' },
    maxlength: { type: Number, value: 200 },
    confirmText: { type: String, value: '确定' },
    loading: { type: Boolean, value: false }
  },
  data: {
    value: ''
  },
  observers: {
    show(visible: boolean) {
      if (visible) {
        this.setData({ value: '' });
      }
    }
  },
  methods: {
    onInput(e: ValueEvent) {
      this.setData({ value: e.detail });
    },
    onCancel() {
      this.triggerEvent('cancel');
    },
    onClose() {
      this.triggerEvent('cancel');
    },
    onConfirm() {
      this.triggerEvent('confirm', { value: this.data.value });
    }
  }
});
