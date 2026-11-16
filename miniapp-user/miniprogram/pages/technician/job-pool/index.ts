import { visitText } from '../../../utils/visitPresentation';
import { listJobPool, PoolEntry } from '../../../services/technician';
import { requireTechnician } from '../../../services/workspace';
import { applianceLabel, displayTime, errorText } from '../../../utils/technicianPresentation';
import { pendingPoolClaims } from '../../../utils/technicianCommand';
type Card=PoolEntry&{title:string;timeText:string;slotText:string};
Page({data:{loading:true,error:'',items:[] as Card[],page:0,more:false,loadingMore:false,moreError:'',total:0,pending:[] as ReturnType<typeof pendingPoolClaims>},_g:0,_owner:'',
 onShow(){this.load();},onPullDownRefresh(){this.load();},onHide(){this._g++;this.setData({items:[],pending:[]});},
 async load(){const g=++this._g;this.setData({loading:true,error:'',items:[],moreError:''});try{await requireTechnician();this._owner=String(wx.getStorageSync('fm.customerId'));if(g!==this._g)return;this.setData({pending:pendingPoolClaims()});const result=await listJobPool();if(g!==this._g||this._owner!==String(wx.getStorageSync('fm.customerId')))return;this.setData({items:result.items.map(x=>({...x,title:applianceLabel(x.applianceType),slotText:visitText(x.preferredVisit),timeText:displayTime(x.publishedAt)})),page:1,more:20<result.total,total:result.total});}catch(e){if(g===this._g)this.setData({error:errorText(e)});}finally{if(g===this._g)this.setData({loading:false});wx.stopPullDownRefresh();}},
 async onReachBottom(){if(this.data.loading||this.data.loadingMore||!this.data.more)return;const g=this._g,owner=this._owner,page=this.data.page+1;this.setData({loadingMore:true,moreError:''});try{const result=await listJobPool(page);if(g!==this._g||owner!==String(wx.getStorageSync('fm.customerId')))return;const map=new Map(this.data.items.map(x=>[x.id,x]));result.items.forEach(x=>map.set(x.id,{...x,title:applianceLabel(x.applianceType),slotText:visitText(x.preferredVisit),timeText:displayTime(x.publishedAt)}));this.setData({items:Array.from(map.values()),page,more:page*20<result.total,total:result.total});}catch(e){if(g===this._g)this.setData({moreError:errorText(e)});}finally{if(g===this._g)this.setData({loadingMore:false});}},
 onOpen(e:WechatMiniprogram.TouchEvent){wx.navigateTo({url:'/pages/technician/job-pool-detail/index?id='+e.currentTarget.dataset.id+'&order='+e.currentTarget.dataset.order});}
});
