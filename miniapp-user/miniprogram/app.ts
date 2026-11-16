import { pruneExpiredPendingBodies } from './utils/technicianCommand';
import { restoreLastWorkspace } from './services/workspace';
App({onLaunch(){pruneExpiredPendingBodies();restoreLastWorkspace().catch(()=>{wx.showToast({title:'维修身份暂无法核实，可在我的页面重试',icon:'none'});});}});
