import { getParticipation } from '../../../services/technician';
import { requireTechnician, enterUser } from '../../../services/workspace';
import { displayTime, presentParticipation, errorText } from '../../../utils/technicianPresentation';
import { backToWorkbench } from '../../../utils/navigation';
Page({data:{id:'',loading:true,error:'',record:null as ReturnType<typeof presentParticipation>|null,title:'',statusText:'',began:'',ended:''},_g:0,
 onLoad(options:Record<string,string>){this.setData({id:options.id||''});},onShow(){this.load();},onPullDownRefresh(){this.load();},onHide(){this._g++;this.setData({record:null});},
 async load(){const g=++this._g;this.setData({loading:true,error:''});try{await requireTechnician();const row=presentParticipation(await getParticipation(this.data.id));if(g!==this._g)return;this.setData({record:row,title:row.title,statusText:row.stateText,began:displayTime(row.participatedAt),ended:row.endedAt?displayTime(row.endedAt):'参与尚未结束'});}catch(error){if(g===this._g)this.setData({error:errorText(error),record:null});}finally{if(g===this._g)this.setData({loading:false});wx.stopPullDownRefresh();}},onBack(){backToWorkbench();},onUser(){enterUser();}
});
