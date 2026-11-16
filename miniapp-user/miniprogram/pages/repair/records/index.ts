import { listRepairRecords, getRepairRequest } from '../../../services/repair';
import { getCurrentProfile } from '../../../services/profile';
import { adoptLegacyRecovery, customerId, openRepair, pendingSubmit } from '../../../utils/repairRecovery';
import { mergeRecords, presentRecord } from '../../../utils/recordPresentation';
import { syncTab } from '../../../utils/navigation';

Page({
  data: { loading: true, refreshing: false, loadingMore: false, error: '', moreError: '',
    group: 'ALL', filters: [{key:'ALL',text:'全部'},{key:'PROCESSING',text:'处理中'},{key:'ENDED',text:'已结束'},{key:'DRAFT',text:'草稿'}],
    items: [] as Array<ReturnType<typeof presentRecord>>, page: 0, total: 0, hasMore: false, pendingId: '', pendingOutsideList: false },
  _generation: 0,
  _owner: '',
  onShow() { syncTab(this,1); this.refresh(); },
  async refresh() {
    if(this._owner && this._owner!==customerId()) this.setData({items:[],page:0,total:0,hasMore:false,pendingId:'',pendingOutsideList:false});
    const generation=++this._generation, targetPages=Math.max(1,this.data.page);
    this.setData({loading:!this.data.items.length,refreshing:true,loadingMore:false,error:'',moreError:''});
    try {
      await getCurrentProfile();
      const owner=customerId();
      if(this._owner && this._owner!==owner) this.setData({items:[],pendingId:'',pendingOutsideList:false});
      this._owner=owner;
      await adoptLegacyRecovery(getRepairRequest);
      let items: Array<ReturnType<typeof presentRecord>>=[], total=0, pages=0;
      for(let page=1;page<=targetPages;page++) {
        const result=await listRepairRecords(page,this.data.group);
        if(generation!==this._generation || owner!==customerId()) return;
        items=mergeRecords(items,result.items.map(presentRecord));total=result.total;pages=page;
        if(page*20>=total) break;
      }
      const pending=pendingSubmit();
      this.setData({items,total,page:pages,hasMore:pages*20<total,pendingId:pending?.draftId||'',
        pendingOutsideList:!!pending && !items.some(item=>item.id===pending.draftId)});
    } catch(error) {
      if(generation===this._generation) this.setData({error:error instanceof Error?error.message:'加载失败，请重试'});
    } finally {
      if(generation===this._generation) this.setData({loading:false,refreshing:false});
      wx.stopPullDownRefresh();
    }
  },
  onPullDownRefresh() { this.refresh(); },
  onFilter(event: WechatMiniprogram.TouchEvent) {
    const group=String(event.currentTarget.dataset.group);
    if(!this.data.filters.some(item=>item.key===group)||group===this.data.group) return;
    this.setData({group,items:[],page:0,total:0,hasMore:false});this.refresh();
  },
  async onReachBottom() {
    if(this.data.refreshing||this.data.loadingMore||!this.data.hasMore) return;
    const generation=this._generation,owner=customerId(),page=this.data.page+1;
    this.setData({loadingMore:true,moreError:''});
    try {
      const result=await listRepairRecords(page,this.data.group);
      if(generation!==this._generation||owner!==customerId()) return;
      this.setData({items:mergeRecords(this.data.items,result.items.map(presentRecord)),page,total:result.total,hasMore:page*20<result.total});
    } catch(error) {
      if(generation===this._generation) this.setData({moreError:error instanceof Error?error.message:'加载失败'});
    } finally { if(generation===this._generation) this.setData({loadingMore:false}); }
  },
  onOpen(event: WechatMiniprogram.TouchEvent) {
    const id=String(event.currentTarget.dataset.id||'');
    if(id) wx.navigateTo({url:'/pages/repair/record-detail/index?id='+encodeURIComponent(id)});
  },
  onRecover() { if(this.data.pendingId) openRepair(this.data.pendingId,'pending'); },
  onNewRepair() { wx.switchTab({url:'/pages/repair/new/index'}); }
});

