import { staffContact } from '../../../utils/technicianPresentation';
import { loadWorkspaceAccess, enterTechnician, enterUser } from '../../../services/workspace';
import { currentBinding, WorkspaceAccess } from '../../../services/technician';
import { technicianMode } from '../../../utils/workspaceStore';
import type { CurrentUserVO, UpdateCurrentUserDTO } from '@fixmate/api-contract';
import Toast from '@vant/weapp/toast/toast';
import { getCurrentProfile, updateCurrentProfile } from '../../../services/profile';
import { uploadAvatarImage } from '../../../services/file';
import { syncTab } from '../../../utils/navigation';
import { customerId, readRecovery, writeRecovery, removeRecovery } from '../../../utils/repairRecovery';
import { newIdempotencyKey } from '../../../utils/id';

interface PendingProfile { dto: UpdateCurrentUserDTO; key: string; at: number }
const PENDING='fm.profileSave';
Page({
  data: {loading:true,error:'',profile:null as CurrentUserVO|null,avatarFailed:false,editing:false,
    nicknameInput:'',avatarPath:'',avatarFileId:'',uploading:false,uploadError:'',avatarUnresolved:false,
    saving:false,saveError:'',pendingSave:false,workspaceMode:'USER',workspace:null as WorkspaceAccess|null,canSwitchWorkspace:false,workspaceLoading:false,workspaceError:'',staffPhoneText:'',staffPhoneSource:'',bindingStatus:''},
  _generation:0,
  _workspaceGeneration:0,
  _owner:'',
  _pending:null as PendingProfile|null,
  onShow() {
    syncTab(this,2);
    this.loadWorkspace();
    if(this._owner&&this._owner!==customerId()) {
      this._generation++;this._pending=null;
      this.setData({profile:null,editing:false,saving:false,uploading:false,pendingSave:false,avatarPath:'',avatarFileId:'',workspace:null,workspaceMode:'USER',canSwitchWorkspace:false,staffPhoneText:'',staffPhoneSource:'',bindingStatus:''});
    }
    if(!this.data.editing&&!this.data.saving&&!this.data.uploading)this.load();
  },
  onPullDownRefresh() {if(this.data.editing)wx.stopPullDownRefresh();else this.load();},
  async load() {
    const generation=++this._generation;this.setData({loading:true,error:'',profile:null});
    try {
      const profile=await getCurrentProfile();if(generation!==this._generation)return;
      this._owner=customerId();
      const pending=readRecovery<PendingProfile>(PENDING);
      this._pending=pending&&typeof pending.key==='string'&&typeof pending.at==='number'&&typeof pending.dto?.nickname==='string'?pending:null;
      this.setData({profile,avatarFailed:false,editing:!!this._pending,pendingSave:!!this._pending,
        nicknameInput:this._pending?.dto.nickname||profile.nickname||'',saveError:this._pending?'上次保存结果待确认，请重试原操作。':''});
    } catch(error) {if(generation===this._generation)this.setData({error:error instanceof Error?error.message:'读取失败，请重试'});}
    finally {if(generation===this._generation)this.setData({loading:false});wx.stopPullDownRefresh();}
  },
  onEdit() {if(this.data.profile)this.setData({editing:true,nicknameInput:this.data.profile.nickname||'',saveError:''});},
  onNickname(event:{detail:{value:string}}) {if(!this.data.pendingSave&&!this.data.saving)this.setData({nicknameInput:event.detail.value});},
  onChooseAvatar(event:{detail:{avatarUrl:string}}) {
    if(this.data.pendingSave||this.data.saving||this.data.uploading||this.data.avatarUnresolved)return;
    const path=event.detail.avatarUrl;if(!path)return;
    this.setData({avatarPath:path,avatarFileId:'',avatarFailed:false,uploadError:''});this.uploadAvatar();
  },
  async uploadAvatar() {
    if(!this.data.avatarPath||this.data.uploading||this.data.pendingSave)return;
    const owner=customerId(),generation=this._generation;
    this.setData({uploading:true,uploadError:''});
    try {
      const file=await uploadAvatarImage(this.data.avatarPath);
      if(owner!==customerId()||generation!==this._generation)return;
      this.setData({avatarFileId:file.id,avatarUnresolved:false});
    } catch(error) {
      if(owner!==customerId()||generation!==this._generation)return;
      const e=error as {code?:string;status?:number;message?:string};
      const unknown=e.code==='NETWORK_ERROR'||(e.status||0)>=500||e.code==='DUPLICATE_REQUEST';
      this.setData({avatarUnresolved:unknown,uploadError:unknown?'头像上传结果待确认，请重试原图片':e.message||'上传失败，请重试'});
    } finally {if(owner===customerId()&&generation===this._generation)this.setData({uploading:false});}
  },
  async onSave() {
    if(this.data.saving||this.data.uploading||!this.data.profile)return;
    if(!this._owner||this._owner!==customerId()) {this.setData({saveError:'登录身份已变化，请重新进入'});return;}
    if(!this._pending) {
      const nickname=this.data.nicknameInput.trim();
      if(!nickname||nickname.length>64) {this.setData({saveError:'请填写1至64个字符的昵称'});return;}
      if(this.data.avatarPath&&!this.data.avatarFileId) {this.setData({saveError:'请先完成头像上传'});return;}
      const dto:UpdateCurrentUserDTO={nickname};if(this.data.avatarFileId)dto.avatarFileId=this.data.avatarFileId;
      this._pending={dto,key:newIdempotencyKey(),at:Date.now()};
      writeRecovery(PENDING,this._pending);this.setData({pendingSave:true});
    }
    const pending=this._pending;
    if(Date.now()-pending.at>30*60*1000) {this.setData({saveError:'保存结果仍未确认，请保留当前记录并联系平台核对；不会自动发起新保存'});return;}
    const owner=customerId(),generation=this._generation;this.setData({saving:true,saveError:''});
    try {
      const profile=await updateCurrentProfile(pending.dto,pending.key);
      if(owner!==customerId()||generation!==this._generation)return;
      removeRecovery(PENDING);this._pending=null;
      this.setData({profile,editing:false,pendingSave:false,avatarPath:'',avatarFileId:'',avatarUnresolved:false,nicknameInput:profile.nickname||'',avatarFailed:false});Toast.success('资料已保存');
    } catch(error) {
      if(owner!==customerId()||generation!==this._generation)return;
      const e=error as {code?:string;status?:number;message?:string};
      if(!e.status||e.status>=500||e.code==='DUPLICATE_REQUEST')this.setData({pendingSave:true,saveError:'保存结果待确认，请重试；系统将恢复原昵称和头像'});
      else {removeRecovery(PENDING);this._pending=null;this.setData({pendingSave:false,saveError:e.message||'保存未完成，请重试'});}
    } finally {if(owner===customerId()&&generation===this._generation)this.setData({saving:false});}
  },
  onCancelEdit() {if(this.data.pendingSave||this.data.uploading||this.data.saving)return;this.setData({editing:false,avatarPath:'',avatarFileId:'',uploadError:'',saveError:'',avatarUnresolved:false});},
  async loadWorkspace() {const generation=++this._workspaceGeneration;this.setData({workspaceLoading:true,workspaceError:'',canSwitchWorkspace:false});try{const access=await loadWorkspaceAccess();const binding=await currentBinding();if(generation!==this._workspaceGeneration)return;const contact=staffContact(access,binding),phone=contact.phone;this.setData({staffPhoneSource:contact.source,bindingStatus:binding?.status||'',workspace:access,workspaceMode:technicianMode()?'TECHNICIAN':'USER',canSwitchWorkspace:access.availableWorkspaces.includes('TECHNICIAN'),staffPhoneText:phone.length>7?phone.slice(0,3)+'****'+phone.slice(-4):phone});syncTab(this,2);}catch(error){if(generation===this._workspaceGeneration)this.setData({workspace:null,workspaceMode:'USER',workspaceError:error instanceof Error?error.message:'维修身份暂无法读取'});}finally{if(generation===this._workspaceGeneration)this.setData({workspaceLoading:false});}},
  async onSwitchWorkspace(){if(this.data.workspaceLoading||this.data.editing||this.data.saving)return;try{if(this.data.workspaceMode==='TECHNICIAN'){enterUser();this.setData({workspaceMode:'USER'});}else if(this.data.canSwitchWorkspace)await enterTechnician();}catch(error){this.setData({workspaceError:error instanceof Error?error.message:'身份核对失败'});}},
  onSchedule(){wx.navigateTo({url:'/pages/technician/schedule/index'});},onCatalog(){wx.navigateTo({url:'/pages/technician/catalog/index'});},onBinding(){wx.navigateTo({url:'/pages/workspace/binding/index'});},
  onAvatarError() {this.setData({avatarFailed:true});}
});
