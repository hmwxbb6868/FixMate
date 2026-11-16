/**
 * U2 信息采集。
 * 阶段 1 只保留路由与骨架：冻结规则 §7 把 /user/triage/** 排除在阶段 1 契约之外，
 * 因此这里不请求任何会话、不展示任何模型输出，也不使用假数据。
 */
Page({
  data: {
    sessionId: ''
  },

  onLoad(options: Record<string, string | undefined>) {
    this.setData({ sessionId: options.sessionId || '' });
  },

  onBackToRepair() {
    wx.navigateBack({
      fail: () => {
        wx.reLaunch({ url: '/pages/repair/new/index' });
      }
    });
  }
});
