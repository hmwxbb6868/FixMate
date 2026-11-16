import {api,raw,setSession,forgetSession,session,preview,Dict,Profile,escape,key,may,operations,schema,schemas,ApiError,journal,Command,commandExpired,sameActor,unknownResult,pathFor,restoreSession} from './core.js';
import {pages,PageDef,visiblePages,orderActions,resourceActions,permitted,Action,workspaces,sidebarPages,pageQuery,validatePageRows,pageReadError,queryMismatchCode,canInviteStaff} from './catalog.js';
import {icon,label,text,dateText,badge,cell,listCell,auditFieldLabel,auditCell,auditChanges,assignmentDisplayStatus,orderDisplayStatus,timelineItems,publicationRequirements,publicationContactReady,orderNextStep,repairNextStep} from './presentation.js';
import {controls,formValue,operationSchema,arrayRow,Choices,validateQuoteInputs} from './forms.js';
import {demoProfile,demoOrders,demoDashboard,demoList,demoDetail} from './preview.js';
const app=document.querySelector<HTMLElement>('#app')!,overlays=document.querySelector<HTMLElement>('#overlays')!;
const actor=()=>preview?demoProfile as Profile:session?.staff||null;
let route='dashboard',page=1,size=10,total=0,rows:Dict[]=[],query:Dict={},dashboard:Dict|null=null,error='',loading=false,epoch=0,dialogEpoch=0,updated='',pending:Command[]=[],menuCollapsed=false;
let repairLookup:Dict|null=null;
let choices:Choices=preview?{technicianId:[{value:'9201',label:'示例师傅甲'},{value:'9202',label:'示例师傅乙'}]}:{},contexts=new Map<string,{action:Action;row:Dict;detail?:Dict;page:string;scope?:string}>();
let timer:ReturnType<typeof setInterval>|undefined;
const def=()=>pages.find(x=>x.id===route)!;
const inviteAction:Action={op:'issueStaffInvitation',label:'签发维修邀请'};
const newInviteAction:Action={op:'issueNewTechnicianInvitation',label:'邀请新人'};
const inviteButton=()=>route==='invitations'&&may(newInviteAction.op,actor())
 ?`<button class="button" data-issue-invite ${preview?'disabled':''}>邀请已有员工</button><button class="button primary" data-invite-new ${preview?'disabled':''}>邀请新人</button>`:'';
function routeFromHash(){const id=location.hash.replace('#/','');return id==='pool-live'?'pool':id;}
const pageTitle=(page:PageDef)=>page.workspace?workspaces[page.workspace]:page.title;
function workspaceNav(page:PageDef,available:PageDef[]){
 if(!page.workspace)return '';
 return `<nav class="workspace-tabs" aria-label="${escape(pageTitle(page))}分类">${available.filter(x=>x.workspace===page.workspace).map(x=>`<button class="button ${x.id===page.id?'selected':''}" data-nav="${x.id}" ${x.id===page.id?'aria-current="page"':''}>${escape(x.title)}</button>`).join('')}</nav>`;
}
const empty=(title:string,description:string)=>`<div class="empty"><div class="empty-icon">${icon('orders')}</div><strong>${escape(title)}</strong><p>${escape(description)}</p></div>`;
function toast(message:string){const el=document.querySelector<HTMLElement>('#toast')!;el.textContent=message;el.classList.add('visible');setTimeout(()=>el.classList.remove('visible'),4000);}
function bindAction(action:Action,row:Dict,detail?:Dict,pageId=route){const id=key();contexts.set(id,{action,row,detail,page:pageId,scope:overlays.children.length?'dialog':'page'});return `<button class="button ${action.danger?'danger':''}" data-command="${id}" ${preview?'disabled title="样式预览不执行业务操作"':''}>${escape(action.label)}</button>`;}
function nav(id:string,filters:Dict={}){if(!visiblePages(actor()!).some(p=>p.id===id))return;epoch++;closeDialog();route=id;page=1;rows=[];total=0;dashboard=null;query=filters;error='';updated='';history.replaceState(null,'','#/'+id);load();}
function render(){
 for(const[k,v]of contexts)if(v.scope==='page')contexts.delete(k);
 if(session?.passwordChangeRequired&&!preview){passwordScreen();return;}
 const profile=actor();if(!profile){login();return;}const navs=visiblePages(profile);if(!navs.length){app.innerHTML=empty('当前岗位暂无管理页面','请联系管理员核对岗位授权');return;}
 if(!navs.some(x=>x.id===route))route=navs[0].id;
 const sidebar=sidebarPages(navs),current=def();
 app.innerHTML=`<div class="shell ${menuCollapsed?'collapsed':''}"><aside class="sidebar"><a class="brand" href="#/dashboard"><span class="brand-symbol">${icon('tool')}</span><span><b>安心修</b><small>运营管理台</small></span></a><nav aria-label="管理功能">${[...new Set(sidebar.map(x=>x.group))].map(group=>`<div class="nav-label">${group}</div>${sidebar.filter(x=>x.group===group).map(x=>`<button class="nav-item ${(x.workspace?x.workspace===current.workspace:route===x.id)?'selected':''}" data-nav="${x.id}">${icon(x.icon)}<span>${escape(pageTitle(x))}</span></button>`).join('')}`).join('')}</nav><div class="sidebar-foot"><span class="online-dot"></span><span>人工服务 · 安心交付</span></div></aside><div class="workspace"><header class="topbar"><button class="icon-button" data-menu aria-label="折叠菜单">${icon('menu')}</button><div class="breadcrumb">工作空间 <span>/</span> <b>${escape(pageTitle(current))}</b></div><div class="top-actions">${preview?'<span class="preview-label">样式预览 · 合成资料</span>':`<button class="button small ${pending.length?'attention':''}" data-pending>${icon('clock')}操作恢复${pending.length?' '+pending.length:''}</button>`}<button class="profile-button" data-password><span class="avatar">${escape(profile.name.slice(0,1))}</span><span>${escape(profile.name)}<small>${escape(text(profile.role))}</small></span></button><button class="icon-button" data-logout aria-label="退出登录">${icon('logout')}</button></div></header><main><div class="page-heading"><div><div class="heading-kicker">${escape(def().group)}</div><h1>${escape(pageTitle(current))}</h1></div><div class="heading-actions"><span class="last-update">${updated?'更新于 '+escape(updated):''}</span><button class="button" data-refresh ${loading?'disabled':''}>${icon('refresh')}刷新</button>${inviteButton()}${def().create&&may(def().create!,profile)?bindAction({op:def().create!,label:'新增'+(route==='staff'?'员工':route==='services'?'服务项目':'价格规则')},{}):''}</div></div>${workspaceNav(current,navs)}${current.workspace||['safety','handoff-history'].includes(route)?`<p class="workspace-description">${escape(current.description)}</p>`:''}${preview?'<div class="preview-note">当前为独立样式预览，所有资料均为合成示例，业务写操作不可用。</div>':''}${error?`<div class="error-banner" role="alert">${escape(error)} <button class="text-button" data-refresh>重新读取</button>${rows.length||dashboard?'<small>当前保留上次成功读取的资料，操作前请刷新核对。</small>':''}</div>`:''}${pending.length&&!preview?`<div class="warning-banner">有 ${pending.length} 项操作结果待核对，请先恢复原操作或核对结果。<button class="text-button" data-pending>查看恢复记录</button></div>`:''}${route==='dashboard'?dashboardHTML():tableHTML()}</main><footer>安心修 FixMate <span>服务运营工作区</span><span>时间均为北京时间</span></footer></div></div>`;
}
const metricLinks:Record<string,{label:string;to:string;filter?:Dict;icon:string}>={manualReviewCount:{label:'待受理报修',to:'intake',filter:{status:'SUBMITTED'},icon:'intake'},assignmentCount:{label:'待分派工单',to:'dispatch',icon:'orders'},upcomingAppointmentCount:{label:'即将上门',to:'orders',filter:{status:'APPOINTED'},icon:'calendar'},quoteConfirmationCount:{label:'待客户确认报价',to:'orders',filter:{status:'WAITING_QUOTE_CONFIRMATION'},icon:'price'},unresolvedSafetyCount:{label:'未解除安全事件',to:'safety',filter:{status:'OPEN'},icon:'shield'},overrideApprovalCount:{label:'价格例外审批',to:'approvals',filter:{status:'PENDING'},icon:'price'},humanHandoffCount:{label:'转人工请求',to:'handoffs',filter:{status:'PENDING'},icon:'users'},urgeCount:{label:'客户催办',to:'urges',filter:{status:'PENDING'},icon:'clock'}};
const followUpKinds:Record<string,{title:string;repair?:boolean;appointment?:boolean}>={
 REPAIR_REVIEW:{title:'报修待核对',repair:true},CUSTOMER_CONFIRM:{title:'待客户确认生成工单',repair:true},
 DISPATCH:{title:'分派待跟进',appointment:true},APPOINTMENT:{title:'上门超时',appointment:true},
 QUOTE_CONFIRM:{title:'报价待跟进'},ACCEPTANCE:{title:'完工待验收'}
};
function followUpPanel(d:Dict){
 const followUps=Array.isArray(d.followUps)?d.followUps:null;
 const groups=Object.entries(followUpKinds).map(([kind,config])=>{
  const items=(followUps||[]).filter((row:Dict)=>row.kind===kind);
  if(!items.length)return '';
  const count=Number(items[0].categoryTotal);
  return `<section class="follow-up-group"><h3>${config.title} <small>共 ${count} 条${count>items.length?' · 显示最早 '+items.length+' 条':''}</small></h3>
    <ul>${items.map((row:Dict)=>{
     const hours=Math.max(0,Math.floor((Date.parse(d.generatedAt)-Date.parse(row.waitingSince))/3600000));
     const duration=hours>=24?Math.floor(hours/24)+' 天 '+hours%24+' 小时':hours+' 小时';
     const owner=({STAFF:'平台',CUSTOMER:'客户',TECHNICIAN:'维修人员'} as Dict)[row.waitingFor]||'待核对';
     return `<li><span>${escape(row.referenceNo)}</span><small>等待${owner} · 已等 ${duration}</small>
       <button class="text-button" ${config.repair?'data-related-repair':'data-related-order'}="${escape(row.targetId)}" ${config.appointment?'data-appointment':''}>查看详情</button></li>`;
    }).join('')}</ul></section>`;
 }).join('');
 const note='报修核对和分派等待满 2 小时、预约结束满 2 小时后提醒。报价等待满 12 小时，客户确认生成工单和验收等待满 24 小时后提醒。';
 return `<section class="panel follow-up-panel"><div class="panel-head"><h2>需要跟进</h2></div><p class="muted">${note}</p>
  ${groups?`<div class="follow-up-groups">${groups}</div>`:followUps?'<p class="muted">暂无达到提醒时间的记录。</p>':'<p class="muted">当前接口未返回跟进资料，请刷新后核对。</p>'}</section>`;
}
function nextStepCard(step:ReturnType<typeof orderNextStep>){
 const action=step.link?`<button class="text-button" data-nav="${escape(step.link)}">前往安全事件</button>`
  :step.tab?`<button class="text-button" data-next-tab="${escape(step.tab)}">查看${({appointment:'预约与分派',quotes:'报价与审批',records:'服务记录',timeline:'处理时间线'} as Dict)[step.tab]}</button>`:'';
 return `<div class="next-step"><strong>下一步 · ${escape(step.owner)}</strong><p>${escape(step.message)}</p>${action}</div>`;
}
function dashboardHTML(){if(!dashboard)return `<section class="panel">${loading?skeleton():empty('工作台暂未读取','请点击刷新重试')}</section>`;
 const d=dashboard,metrics=Object.entries(metricLinks).filter(([,v])=>visiblePages(actor()!).some(x=>x.id===v.to));
 return `<div class="dashboard-overview"><div><h2>每一项服务，都有下一步</h2><p>集中处理受理、派单和异常，让服务有序推进。</p><small>数据时间 ${escape(dateText(d.generatedAt))}</small></div><div class="overview-symbol">${icon('shield')}</div></div><div class="metrics">${metrics.slice(0,4).map(([k,v])=>`<button class="metric" data-metric="${k}"><div class="metric-label">${v.label}<span>${icon(v.icon)}</span></div><div class="metric-value">${Number(d[k]||0)}<small>项</small></div><div class="metric-link">进入处理 ${icon('arrow')}</div></button>`).join('')}</div><div class="dashboard-grid"><section class="panel"><div class="panel-head"><h2>优先处理</h2><span class="subtle">业务待办</span></div><div class="todo-grid">${metrics.slice(4).map(([k,v])=>`<button class="todo-card" data-metric="${k}"><span class="todo-icon ${k==='unresolvedSafetyCount'?'risk':''}">${icon(v.icon)}</span><span><strong>${v.label}</strong><small>核实依据后处理</small></span><b>${Number(d[k]||0)}</b>${icon('arrow')}</button>`).join('')}</div><div class="dashboard-reminders"><span>退回待审 <b>${Number(d.returnRequestCount||0)}</b></span><span>待重新报价 <b>${Number(d.requoteCount||0)}</b></span><span>爽约待协调 <b>${Number(d.missedAppointmentCount||0)}</b></span></div></section><section class="panel distribution"><div class="panel-head"><h2>工单进度分布</h2><span class="subtle">当前状态</span></div>${Object.entries(d.statusDistribution||{}).map(([s,count])=>`<button class="distribution-row" data-status="${escape(s)}"><span>${escape(text(s))}</span><div class="bar"><i style="width:${Math.max(2,Number(count)/Math.max(1,...Object.values(d.statusDistribution).map(Number))*100)}%"></i></div><b>${Number(count)}</b></button>`).join('')}</section></div>${followUpPanel(d)}<section class="panel shortcuts"><div><h2>常用工作入口</h2><p>按业务查看资料，按权限完成处理</p></div>${['orders','intake','staff','services','audit'].filter(id=>visiblePages(actor()!).some(x=>x.id===id)).map(id=>{const p=pages.find(x=>x.id===id)!;return `<button class="button" data-nav="${id}">${icon(p.icon)}${escape(pageTitle(p))}${icon('arrow')}</button>`;}).join('')}</section>`;
}
function filterControl(name:string){const param=operations[def().op].params.find((p:Dict)=>p.name===name),s=schema(param?.schema||{});let options=s.enum?.filter((v:unknown)=>v!=='ALL').map((v:any)=>({value:String(v),label:name==='status'&&v==='WAITING_ASSIGNMENT'?'待分派（含待师傅接单）':text(v)}));if(name==='technicianId'||name==='staffId')options=choices.technicianId;if(name==='riskFlag')options=[{value:'true',label:'有风险'},{value:'false',label:'无风险'}];
 const defaultLabel=name==='status'&&route==='approvals'?'待审批':name==='status'&&['handoffs','urges'].includes(route)?'待处理':'全部';
 const attrs=`name="${escape(name)}"`;return `<label class="filter-field"><span>${escape(name==='keyword'?'关键词':label(name))}</span>${options?`<select ${attrs}><option value="">${defaultLabel}</option>${options.filter((x:Dict)=>defaultLabel==='全部'||x.value!=='PENDING').map((x:any)=>`<option value="${escape(x.value)}" ${query[name]===x.value?'selected':''}>${escape(x.label)}</option>`).join('')}</select>`:`<input ${attrs} type="${name==='appointmentDate'?'date':'text'}" value="${escape(query[name])}" placeholder="${name==='keyword'?(route==='customers'?'搜索用户编号或昵称':route==='quote-records'?'搜索维修工单号或报价单号':'搜索维修工单号、完整报修号或故障'):'请输入'+escape(label(name))}">`}</label>`;
}
function skeleton(){return '<div class="skeleton" role="status">正在读取最新资料…<div></div><div></div><div></div></div>';}
function repairLookupCard(repair:Dict){
 const step=repairNextStep({status:repair.repairStatus});
 return `<div class="repair-lookup"><div><strong>${escape(repair.repairNo)}</strong> ${badge(repair.repairStatus)}</div>
  <p>${escape(step.message)}</p><button class="button" data-related-repair="${escape(repair.id)}">查看报修详情</button></div>`;
}
function tableHTML(){
 const p=def(),filters=p.filters||[];
 const fieldLabel=p.id==='audit'?auditFieldLabel:(field:string)=>p.op==='listAdminWorkOrders'&&field==='status'?'工单状态':field==='currentAssigneeName'?'分派对象':label(field);
 const renderCell=p.id==='audit'?auditCell:listCell;
 const cellValue=(row:Dict,field:string)=>{
  if(field==='status'&&p.id==='quote-rejected')return row.status==='WAITING_QUOTE'?'待师傅重报':'待平台协调';
  if(field==='status'&&p.op==='listAdminWorkOrders')return orderDisplayStatus(row.status,row.assignmentStatus||row.latestAssignmentStatus);
  if(field==='coordinationReason')return [row.coordinationCause&&text(row.coordinationCause),row.coordinationReason].filter(Boolean).join(' · ');
  if(field==='repairStatus'&&p.id==='intake-reviewed'&&row.workOrderStatus==='WAITING_CUSTOMER_VISIT')return row.workOrderStatus;
  return row[field];
 };
 const noRows=p.id==='pool'?'暂无可接工单':p.id==='handoff-history'?'暂无符合筛选条件的人工请求':'暂无相关记录';
 const noRowsHint=p.id==='pool'?'待分派的工单可从「员工待办 → 待分派」办理发布。':'可调整筛选条件后重新查询';
 const canPublishFromRow=(row:Dict)=>p.id==='pool-direct'&&row.latestAssignmentStatus==='DECLINED'
  &&row.status==='WAITING_ASSIGNMENT'&&!row.currentAssigneeId&&!row.poolPublished&&may('adminPublishJobPool',actor());
 return `<section class="panel table-panel">${filters.length?`<form id="filters" class="filters">${filters.map(filterControl).join('')}<div class="filter-actions"><button class="button primary" type="submit">${icon('search')}查询</button><button class="button" type="button" data-reset>重置</button></div></form>`:''}<div class="table-toolbar"><div><strong>${p.title}列表</strong><span class="subtle">${repairLookup?"找到 1 条尚未生成工单的报修":"共 "+total+" 条记录"}</span></div><span class="subtle">${actor()?.role==='AUDITOR'?'审计只读':''}</span></div>${repairLookup?repairLookupCard(repairLookup):loading&&!rows.length?skeleton():!rows.length?empty(error?'资料暂不可用':noRows,error?'请重试读取，查询失败不会显示为正常空列表':noRowsHint):`<div class="table-scroll"><table><thead><tr>${p.columns.map(k=>`<th>${escape(fieldLabel(k))}</th>`).join('')}<th class="operation-col">操作</th></tr></thead><tbody>${rows.map((row,i)=>`<tr>${p.columns.map(k=>`<td class="${k==='symptomSummary'||k==='description'||k==='reason'||k==='overrideReason'||k==='handlingConclusion'||k==='coordinationReason'?'summary-cell':''}">${k==='workOrderNo'&&p.id==='orders'&&row.repairNo?`${renderCell(k,row[k])}<small class="related-repair-no">报修号 ${escape(row.repairNo)}</small>`:p.id==='staff'&&k==='phone'&&!row.phone?.trim()&&row.bindingApplicationPhone?escape(row.bindingApplicationPhone)+' <small class="muted">（绑定申请自填）</small>':renderCell(k,cellValue(row,k))}</td>`).join('')}<td class="operation-col"><div class="table-row-actions"><button class="table-link" data-open="${i}">查看详情 ${icon('arrow')}</button>${canPublishFromRow(row)?`<button class="table-link" data-publish-row="${i}">发布到接单大厅</button>`:''}</div></td></tr>`).join('')}</tbody></table></div>`}<div class="pagination"><span>第 ${total?(page-1)*size+1:0}–${Math.min(page*size,total)} 条，共 ${total} 条</span><div><select id="page-size" aria-label="每页条数">${[10,20,50].map(n=>`<option ${size===n?'selected':''}>${n}</option>`).join('')}</select><button class="button small" data-page="${page-1}" ${page===1||loading?'disabled':''}>上一页</button><b>${page}</b><button class="button small" data-page="${page+1}" ${page*size>=total||loading?'disabled':''}>下一页</button></div></div></section>`;}
function previewPage(filters:Dict){const all=demoList(route).filter(row=>Object.entries(filters).every(([name,value])=>{if(!value||name==='queue'||name==='pendingReview'||name==='pendingCustomerConfirmation'||(name==='status'&&value==='ALL'))return true;if(name==='keyword')return [row.workOrderNo,row.repairNo,row.symptomSummary,row.applianceType].some(x=>String(x||'').toLowerCase().includes(String(value).toLowerCase()));if(name==='appointmentDate')return row.appointmentStartAt&&dateText(row.appointmentStartAt).slice(0,10)===value;if(name==='technicianId')return row.currentAssigneeId===value;if(name==='riskFlag')return String(!!row.riskFlag)===value;return String(row[name]??'')===String(value);}));return {items:all.slice((page-1)*size,page*size),total:all.length};}
async function readTable(pageDef:PageDef,filters:Dict){
 try{
  const result=await api(pageDef.op,{},undefined,undefined,{...filters,page,size});
  validatePageRows(filters,result.items||[]);
  return result;
 }catch(error){throw pageReadError(pageDef,filters,error);}
}
async function load(){const generation=++epoch,profile=actor();if(!profile)return;loading=true;repairLookup=null;render();
 try{
  if(route==='dashboard'){
   const data=preview?demoDashboard:await api(def().op);
   if(generation!==epoch||actor()?.id!==profile.id)return;
   dashboard=data;
  }else{
   const pageDef=def(),filters=pageQuery(pageDef,query);
   const result=preview?previewPage(filters):await readTable(pageDef,filters);
   if(generation!==epoch||actor()?.id!==profile.id)return;
   rows=result.items||[];total=Number(result.total||0);
   const repairNo=String(filters.keyword||'').trim();
   if(pageDef.id==='orders'&&!total&&/^RR[0-9A-Z]{1,30}$/i.test(repairNo)
     &&Object.entries(filters).every(([key,value])=>key==='keyword'||!value)){
    const matches=preview?[...demoList('intake'),...demoList('intake-reviewed')].filter(x=>x.repairNo.toUpperCase()===repairNo.toUpperCase())
     :(await api('listAdminRepairRequests',{},undefined,undefined,{repairNo,page:1,size:10})).items;
    if(generation!==epoch||actor()?.id!==profile.id)return;
    repairLookup=matches.find((row:Dict)=>row.workOrderId==null&&String(row.repairNo).toUpperCase()===repairNo.toUpperCase())||null;
   }
   if(page>1&&!rows.length&&(page-1)*size>=total){page=Math.max(1,Math.ceil(total/size));await load();return;}
  }
  error='';updated=dateText(new Date().toISOString());
 }
 catch(e){if(generation===epoch){error=e instanceof Error?e.message:'读取失败';if(e instanceof ApiError&&([401,403].includes(e.status)||e.code===queryMismatchCode)){rows=[];dashboard=null;total=0;}}}
 finally{if(generation===epoch){loading=false;render();}}
}
function dialog(title:string,content:string,wide=false){closeDialog();const id=++dialogEpoch;overlays.innerHTML=`<dialog class="modal ${wide?'wide':''}" aria-labelledby="modal-title"><div class="modal-head"><div><small>安心修 · ${escape(def()?.title||'管理工作区')}</small><h2 id="modal-title">${escape(title)}</h2></div><button class="icon-button" data-close aria-label="关闭">${icon('close')}</button></div><div class="modal-body">${content}</div></dialog>`;const el=overlays.querySelector<HTMLDialogElement>('dialog')!;el.addEventListener('cancel',e=>{if(el.dataset.busy==='true')e.preventDefault();else closeDialog();});el.showModal();return id;}
function closeDialog(){const el=overlays.querySelector<HTMLDialogElement>('dialog');if(el?.dataset.busy==='true')return;dialogEpoch++;el?.close();overlays.innerHTML='';activeDetail=null;for(const[k,v]of contexts)if(v.scope==='dialog')contexts.delete(k);}
function errorInDialog(message:string){const target=overlays.querySelector<HTMLElement>('.form-error');if(target)target.textContent=message;else toast(message);}
const hiddenKeys=new Set(['allowedActions','contentHash','quoteContentHash','mediaFiles','url','avatarUrl','openId','openid','passwordHash']);
function detailGrid(row:Dict,keys?:string[],isAudit=false):string {
 const fieldLabel=isAudit?auditFieldLabel:label;
 const renderCell=isAudit?auditCell:cell;
 return `<dl class="detail-grid">${(keys||Object.keys(row)).filter(k=>!hiddenKeys.has(k)).map(k=>{
  const value=row[k];
  if(value===undefined)return '';
  const nested=value!==null&&typeof value==='object';
  const full=nested||['facts','summary','reason','description','symptomDescription','reviewNote','manualReviewNote','handlingConclusion'].includes(k);
  let content:string;
  if(Array.isArray(value)){
   content=value.length?value.map(item=>item&&typeof item==='object'?detailGrid(item,undefined,isAudit):escape(text(item))).join(''):'<span class="muted">暂无记录</span>';
  }else{
   content=nested?detailGrid(value,undefined,isAudit):renderCell(k,value);
  }
  return `<div class="${full?'full':''}"><dt>${escape(fieldLabel(k))}</dt><dd>${content}</dd></div>`;
 }).join('')}</dl>`;
}
function gallery(files:Dict[]=[]){const safe=files.filter(x=>{try{return new URL(x.url).protocol==='https:';}catch{return false;}});return safe.length?`<div class="photo-grid">${safe.map(x=>`<a href="${escape(x.url)}" target="_blank" rel="noopener noreferrer" referrerpolicy="no-referrer"><img src="${escape(x.url)}" alt="客户故障照片" referrerpolicy="no-referrer"><span>打开照片</span></a>`).join('')}</div>`:'';}
function section(title:string,content:string){return `<section class="detail-section"><h3>${escape(title)}</h3>${content}</section>`;}
function auditDetail(row: Dict): string {
 const summary = {...row.summary, proxyTechnicianId: row.proxyTechnicianId};
 const fields = ['action', 'occurredAt', 'resourceType', 'resourceId', 'operatorType', 'operatorId', 'id'];
 if (summary.reason) fields.push('reason');
 if (summary.proxyTechnicianId) fields.push('proxyTechnicianId');
 const changes = auditChanges(row.beforeSummary, row.afterSummary);
 const details = changes.map(({label, value}) =>
  `<div><dt>${escape(label)}</dt><dd>${escape(value)}</dd></div>`
 ).join('');
 return section('操作信息', detailGrid(summary, fields, true)) +
  (details ? section('变更内容', `<dl class="detail-grid">${details}</dl>`) : '');
}

function basicDetail(pageId:string,row:Dict){
 if(pageId==='audit')return auditDetail(row);
 if(pageId==='intake'||pageId==='intake-reviewed'){
  const repair=row.repairRequest||{};
  return nextStepCard(repairNextStep(repair,row.allowedActions))
   +section('报修信息',detailGrid(repair,['repairNo','status','applianceType','brandCode','modelCode','symptomDescription','errorCodeText','occurredAt','safetyAnswers','version'])+gallery(repair.mediaFiles))
   +section('服务联系',detailGrid(row.serviceContact||{}))+section('人工核对',detailGrid(row,['manualReviewedBy','manualReviewedAt','manualReviewNote']));
 }
 return section('资料详情',detailGrid(row));
}
async function openRow(index:number,publishRequested=false){const original=rows[index];if(!original)return;const p=def(),generation=epoch,stamp=dialog(p.title+'详情',skeleton(),true);let row:Dict;
 try{row=preview?demoDetail(p.op==='listAdminWorkOrders'?'orders':p.op==='listAdminRepairRequests'?'intake':route,original):p.detail?await api(p.detail,{id:original.id}):original;if(stamp!==dialogEpoch||generation!==epoch)return;
 if(p.op==='listAdminWorkOrders'){renderOrderDetail(row,stamp,{publishRequested});return;}
 const actions=permitted(resourceActions(route,row),row,actor());
 const orderId=row.workOrderId||row.repairRequest?.workOrderId;
 overlays.querySelector('.modal-body')!.innerHTML=`${basicDetail(p.op==='listAdminRepairRequests'?'intake':route,row)}<div class="modal-actions">${actions.map(a=>bindAction(a,row)).join('')}${orderId&&may('getAdminWorkOrder',actor())?`<button class="button" data-related-order="${escape(orderId)}">打开关联工单</button>`:''}<button class="button" data-close>关闭</button></div>`;
 }catch(e){if(stamp===dialogEpoch)overlays.querySelector('.modal-body')!.innerHTML=empty('详情读取失败',e instanceof Error?e.message:'请关闭后重试');}}

async function relatedOrder(id:string,openAppointment=false){
 const stamp=dialog('关联工单',skeleton(),true);
 try{
  const example=preview?demoOrders.find(x=>x.id===id):null;
  const detail=preview?(example?demoDetail('orders',example):null):await api('getAdminWorkOrder',{id});
  if(stamp!==dialogEpoch)return;
  if(detail)renderOrderDetail(detail,stamp,{openAppointment});
  else overlays.querySelector('.modal-body')!.innerHTML=empty('暂无示例工单','正式数据以管理端实际记录为准');
 }catch(e){if(stamp===dialogEpoch)overlays.querySelector('.modal-body')!.innerHTML=empty('工单读取失败',(e as Error).message);}
}
let activeDetail:Dict|null=null;
function renderOrderDetail(detail:Dict,stamp:number,{publishRequested=false,openAppointment=false}={}){
 if(stamp!==dialogEpoch)return;activeDetail=detail;const w=detail.workOrder||{};
 const quoteRejected=['INITIAL_QUOTE_REJECTED','ADDITIONAL_QUOTE_REJECTED'].includes(detail.coordinationCause);
 const returnRequested=detail.assignment?.status==='RETURN_REQUESTED';
 const actions=permitted(orderActions,detail,actor()).filter(action=>action.op!=='adminCoordination'||detail.allowedCoordinationResolutions?.length)
  .filter(action=>!returnRequested||action.op!=='assignWorkOrder')
  .filter(action=>!quoteRejected||action.op==='adminCoordination')
  .map(action=>{
   if(action.op==='assignWorkOrder'&&detail.reworkTechnicianId)return {...action,label:'定向派给原维修人员'};
   if(action.op==='assignWorkOrder')return {...action,label:detail.assignment&&['PENDING','ACTIVE','RETURN_REQUESTED'].includes(detail.assignment.status)?'改派':'定向派单'};
   if(action.op==='adminCoordination'&&detail.allowedCoordinationResolutions.length===1&&detail.allowedCoordinationResolutions[0]==='CONTINUE_AFTER_SAFETY')
    return {...action,label:'继续安排维修'};
   return action;
  });
 if(returnRequested){
  const returnActions:Action[]=[
   {op:'adminPublishJobPool',label:'批准退回并发布到接单大厅',action:'APPROVE_RETURN_TO_POOL'},
   {op:'decideAssignmentReturn',label:'批准退回并改派',action:'DECIDE_RETURN',seed:{approved:true}},
   {op:'decideAssignmentReturn',label:'驳回退回申请',action:'DECIDE_RETURN',seed:{approved:false}}
  ];
  actions.unshift(...permitted(returnActions,detail,actor()).filter(action=>action.seed?.approved!==true
   ||detail.allowedActions.includes('ASSIGN')));
 }
 const timeline=timelineItems(w.timeline);
 const coordinationNote=detail.coordinationCause?section('协调原因',
  `<p><strong>${escape(text(detail.coordinationCause))}</strong></p>${detail.coordinationReason?`<p>${escape(detail.coordinationReason)}</p>`:''}
  ${detail.coordinationCause==='ACCEPTANCE_ISSUE'?detailGrid(detail,['preferredVisit']):''}`):'';
 const firstTab=route==='quote-rejected'?'quotes':openAppointment||publishRequested||(def().fixedQuery?.queue&&route!=='coordination')?'appointment':'overview';
 const tabs=[['overview','工单概况'],['appointment','预约与分派'],['quotes','报价与审批'],['records','服务记录'],['timeline','处理时间线']];
 const nextStep=route==='dispatch'?'':`<div id="order-next-step">${nextStepCard(orderNextStep(detail,undefined))}</div>`;
 overlays.querySelector('.modal-body')!.innerHTML=`<div class="order-hero"><div><small>工单号 ${escape(w.workOrderNo)}</small>${detail.repairNo?`<small class="related-repair-no">报修号 ${escape(detail.repairNo)}</small>`:''}<h2>${escape(text(w.applianceType))}维修 ${badge(orderDisplayStatus(w.status,detail.assignment?.status))}</h2><p>${escape(w.symptomDescription||'')}</p></div><span class="hero-symbol">${icon('tool')}</span></div>${coordinationNote}${nextStep}<div class="detail-tabs">${tabs.map(([id,t])=>`<button data-detail-tab="${id}" class="${firstTab===id?'selected':''}">${t}</button>`).join('')}</div><div data-detail-panel="overview" ${firstTab==='overview'?'':'hidden'}>${section('设备与报修信息',detailGrid(w,['applianceType','brandCode','modelCode','errorCodeText','occurredAt','version'])+gallery(w.mediaFiles))}${section('服务联系',detailGrid(detail.serviceContact||{}))}<div class="inline-actions"><button class="button" data-related-repair="${escape(detail.repairRequestId)}">查看关联报修</button></div></div><div data-detail-panel="appointment" ${firstTab==='appointment'?'':'hidden'}>${section('客户期望时段',detailGrid(detail,['visitPolicy','preferredVisit']))}${section('当前预约',w.appointment?detailGrid(w.appointment):'<p class="muted">暂无已建立的预约。新单需客户选择时段，师傅确认接单后预约成立。</p>')}${section('当前分派',detail.assignment?detailGrid({...detail.assignment,status:assignmentDisplayStatus(detail.assignment.status)}):'<p class="muted">暂无当前分派。</p>')}<div id="pool-info"></div><div class="inline-actions"><button class="button" data-assignment-history="${escape(w.id)}">查看分派历史</button></div></div><div data-detail-panel="quotes" ${firstTab==='quotes'?'':'hidden'}><div id="quote-content">${preview?empty('暂无示例报价','正式报价由服务器计算并保留版本'):skeleton()}</div></div><div data-detail-panel="records" hidden>${section('服务事实与修正链',detail.serviceRecords?.length?detail.serviceRecords.map((r:Dict)=>detailGrid(r)).join('<hr>'):empty('暂无服务记录','师傅提交检测与维修事实后显示'))}</div><div data-detail-panel="timeline" hidden>${section('处理时间线',timeline.length?`<ol class="timeline">${timeline.map(e=>`<li><i></i><time>${escape(e.time)}</time><strong>${escape(e.title)}</strong>${e.detail?`<p>${escape(e.detail)}</p>`:''}</li>`).join('')}</ol>`:empty('暂无时间线','以服务器记录为准'))}</div><div class="modal-actions sticky">${actions.map(a=>bindAction(a,detail,detail)).join('')}<button class="button" data-close>关闭</button></div>`;
 if(!preview){loadQuotes(detail,stamp);loadPool(detail,stamp,publishRequested);}
}
async function loadQuotes(detail:Dict,stamp:number){try{const list=await api('adminListQuotes',{id:detail.workOrder.id});if(stamp!==dialogEpoch)return;const target=overlays.querySelector('#quote-content')!;target.innerHTML=list.length?list.map((q:Dict)=>{const quote=q.quote;const actions:Action[]=[{op:'adminUpdateQuote',label:'编辑草稿',action:'EDIT'},{op:'adminSubmitQuote',label:'提交报价',action:'SUBMIT'},{op:'adminWithdrawQuote',label:'撤回报价',action:'WITHDRAW',danger:true},{op:'adminRequestQuoteOverride',label:'申请超区间审批',action:'REQUEST_OVERRIDE'},{op:'approveQuoteOverride',label:'批准超区间',action:'APPROVE_OVERRIDE'},{op:'rejectQuoteOverride',label:'拒绝超区间',action:'REJECT_OVERRIDE',danger:true}];return `<section class="quote-card"><div class="quote-head"><h3>${escape(quote.quoteNo)} <small>第 ${Number(quote.versionNo)} 版 · ${escape(text(quote.quoteType))}</small></h3>${badge(quote.status)}</div><div class="amounts"><div><small>此前已确认金额</small><strong>¥ ${escape(quote.confirmedAmountBefore)}</strong></div><div><small>本次追加金额</small><strong>¥ ${escape(quote.currentAdditionalAmount)}</strong></div><div class="total"><small>接受后累计应付</small><strong>¥ ${escape(quote.payableTotalAfterAcceptance)}</strong></div></div>${detailGrid(quote,['changeReason','expiresAt','submittedAt'])}<div class="table-scroll"><table><thead><tr><th>项目</th><th>费用类型</th><th>数量</th><th>单价</th><th>金额</th></tr></thead><tbody>${quote.items.map((item:Dict)=>`<tr><td>${escape(item.itemName)}</td><td>${escape(text(item.feeType))}</td><td>${escape(item.quantity)}</td><td>${escape(item.unitPrice||item.selectedUnitPrice)}</td><td>${escape(item.lineAmount||item.amount)}</td></tr>`).join('')}</tbody></table></div>${q.approval?section('审批记录',detailGrid(q.approval)):''}<div class="inline-actions">${permitted(actions,q,actor()).map(a=>bindAction(a,q,detail,'quotes')).join('')}</div></section>`;}).join(''):empty('暂无报价','先提交现场检测记录，再根据可用动作创建报价');}catch(e){if(stamp===dialogEpoch)overlays.querySelector('#quote-content')!.innerHTML=`<div class="error-banner">${escape((e as Error).message)}，请关闭详情后重新读取。</div>`;}}
async function loadPool(detail:Dict,stamp:number,publishRequested=false){
 if(detail.workOrder.status==='CANCELLED')return;
 if(!may('adminGetJobPoolPublication',actor()))return;
 try{
  let pool:Dict|null=null;
  try{pool=await api('adminGetJobPoolPublication',{id:detail.workOrder.id});}
  catch(error){if(!(error instanceof ApiError&&error.status===404&&error.code==='RESOURCE_NOT_FOUND'&&error.message==='工单尚无接单池发布记录'))throw error;}
  if(stamp!==dialogEpoch)return;
  const target=overlays.querySelector<HTMLElement>('#pool-info')!;
  const nextStep=overlays.querySelector<HTMLElement>('#order-next-step');
  if(nextStep)nextStep.innerHTML=nextStepCard(orderNextStep(detail,pool));
  const missing=publicationRequirements(detail),published=pool?.status==='PUBLISHED';
  const canOfferPublish=detail.workOrder.status==='WAITING_ASSIGNMENT'&&!detail.assignment&&may('adminPublishJobPool',actor());
  const canPublish=!published&&canOfferPublish&&!missing.length&&publicationContactReady(detail.serviceContact)
   &&detail.safetyBlocked!==true&&detail.allowedActions?.includes('ASSIGN');
  const publish:Action={op:'adminPublishJobPool',label:'发布到接单大厅'};
  if(publishRequested&&canPublish){await openCommand({action:publish,row:detail,detail,page:route});return;}
  let button='';
  if(published&&pool&&may('adminWithdrawJobPool',actor())){
   button=bindAction({op:'adminWithdrawJobPool',label:'撤回大厅发布',args:{id:pool.id},seed:{expectedPublicationVersion:pool.version},danger:true},detail,detail);
  }else if(canPublish)button=bindAction(publish,detail,detail);
  else if(!published&&canOfferPublish)button='<button class="button" disabled aria-describedby="pool-requirements">发布到接单大厅</button>';
  const contactLink=canOfferPublish&&!published&&!publicationContactReady(detail.serviceContact)&&detail.repairRequestId&&may('getAdminRepairRequest',actor())
   ?`<button class="button" data-related-repair="${escape(detail.repairRequestId)}" data-from-order="${escape(detail.workOrder.id)}">核对服务联系</button>`:'';
  const hint=detail.safetyBlocked?'安全事件尚未解除。':published&&!missing.length?'':missing.join(' ')||(canPublish?'条件已齐全，可核对并发布。':'请核对工单状态和当前岗位权限。');
  target.innerHTML=section('自主接单发布',(pool?detailGrid(pool):'')+
   (hint?`<p id="pool-requirements" class="muted">${escape(hint)}</p>`:''))+
   (button||contactLink?`<div class="inline-actions">${contactLink}${button}</div>`:'');
  if(publishRequested)target.scrollIntoView({block:'nearest'});
 }catch(error){
  if(stamp===dialogEpoch)overlays.querySelector('#pool-info')!.innerHTML=`<div class="error-banner">接单大厅发布状态读取失败：${escape((error as Error).message)}</div>`;
 }
}
async function relatedRepair(id:string,fromOrderId?:string){
 const stamp=dialog('关联报修',skeleton(),true);
 const back=fromOrderId?`<button class="button" data-related-order="${escape(fromOrderId)}" data-appointment>返回预约与分派</button>`:'';
 try{
  const example=preview?[...demoList('intake'),...demoList('intake-reviewed')].find(row=>row.id===id):null;
  const d=preview?demoDetail('intake',example||demoList('intake')[0]):await api('getAdminRepairRequest',{id});
  if(stamp!==dialogEpoch)return;
  const actions=permitted(resourceActions('intake',d),d,actor());
  const contactHint=fromOrderId&&!publicationContactReady(d.serviceContact)
   ?(actions.some(a=>a.op==='confirmRepairServiceContact')
    ?'请核对下方的联系人、电话和上门地址，再点「确认服务联系」。'
    :'当前资料无法在这里确认。请核实联系人、联系电话和原报修上门地址。'):'';
  const orderId=d.repairRequest?.workOrderId;
  const openOrder=!back&&orderId&&may('getAdminWorkOrder',actor())
   ?`<button class="button" data-related-order="${escape(orderId)}">打开关联工单</button>`:'';
  overlays.querySelector('.modal-body')!.innerHTML=(contactHint?`<p class="form-notice">${escape(contactHint)}</p>`:'')+basicDetail('intake',d)
   +`<div class="modal-actions">${actions.map(a=>bindAction(a,d,undefined,'intake')).join('')}${back||openOrder}<button class="button" data-close>关闭</button></div>`;
 }catch(e){if(stamp===dialogEpoch)overlays.querySelector('.modal-body')!.innerHTML=empty('读取失败',(e as Error).message)+`<div class="modal-actions">${back}<button class="button" data-close>关闭</button></div>`;}
}
async function historyDetail(id:string){const stamp=dialog('分派历史',skeleton(),true);try{const result=await api('listWorkOrderAssignments',{id},undefined,undefined,{page:1,size:100});if(stamp===dialogEpoch)overlays.querySelector('.modal-body')!.innerHTML=result.items.length?'<p class="muted">这里记录每次派给谁、何时接单或结束，以及拒接、改派等结果。</p>'+result.items.map((row:Dict)=>section(text(assignmentDisplayStatus(row.status)),detailGrid(row)+`<button class="button" data-history-detail="${escape(row.id)}">查看历史详情</button>`)).join(''):'<p class="muted">暂无分派历史</p>';}catch(e){if(stamp===dialogEpoch)overlays.querySelector('.modal-body')!.innerHTML=empty('读取失败',(e as Error).message);}}
async function optionsAll(op:string){const result:Dict[]=[];let p=1;while(p<=100){const data=await api(op,{},undefined,undefined,{page:p,size:100});if(Array.isArray(data))return data;result.push(...data.items);if(result.length>=Number(data.total))return result;p++;}throw Error('可选资料较多，请先核对配置范围');}
async function chooseInvitationRecipient(){
 const stamp=dialog('签发维修邀请',skeleton());
 const body=overlays.querySelector<HTMLElement>('.modal-body')!;
 try{
  const staff=(await optionsAll('listStaff')).filter(canInviteStaff);
  if(stamp!==dialogEpoch)return;
  if(!staff.length){
   body.innerHTML=empty('暂无可邀请的维修人员','请先新增启用的维修人员。已有待审核申请或有效绑定的人员可到「维修身份审核」查看。')+
    '<div class="modal-actions"><button class="button" data-nav="staff">打开员工档案</button><button class="button" data-close>关闭</button></div>';
   return;
  }
  body.innerHTML=`<form id="invitation-recipient"><p class="form-notice">邀请码发给选中的师傅本人，24 小时内可使用一次。</p>
   <label class="field"><span>维修人员</span><select name="staffId" required><option value="">请选择维修人员</option>
    ${staff.map(s=>`<option value="${escape(s.id)}">${escape(s.name)} · ${escape(s.username)}</option>`).join('')}</select></label>
   <div class="modal-actions"><button class="button" type="button" data-close>取消</button><button class="button primary" type="submit">继续</button></div></form>`;
  body.querySelector<HTMLFormElement>('form')!.onsubmit=e=>{
   e.preventDefault();
   const selected=new FormData(e.currentTarget as HTMLFormElement).get('staffId');
   const row=staff.find(s=>s.id===selected);
   if(row)openCommand({action:inviteAction,row,page:route});
  };
 }catch(e){if(stamp===dialogEpoch)body.innerHTML=empty('员工资料暂不可用',(e as Error).message);}
}
function showInvitationReceipt(result:Dict){
 const expires=dateText(result.invitation?.expiresAt);
 const link=result.invitationUrl;
 const newcomerCode=!link&&!result.invitation?.staffId;
 const message=link||`请打开「安心修」微信小程序，进入「我的 → 维修身份申请」，粘贴下方邀请码，填写本人姓名和联系电话后提交。\n\n邀请码：${result.invitationCode}\n有效期至：${expires}（北京时间）\n\n平台核实通过后，可在「我的」进入维修工作区。`;
 dialog('维修邀请已签发',`<div class="invitation-receipt"><p>${link?`请将链接私发给新人本人。有效期至 ${escape(expires)}（北京时间），仅可提交一次。`:newcomerCode?'微信暂未开放该小程序的链接权限，已签发一次性邀请码。请复制下方邀请说明，私发给新人本人。':'请把下面的邀请说明私发给师傅本人。邀请码仅在签发回执中展示。'}</p>
  <textarea id="invitation-message" aria-label="可复制的维修邀请${link?'链接':'说明'}" rows="${link?3:7}" readonly>${escape(message)}</textarea>
  <div class="form-error" role="status"></div>
  <div class="modal-actions"><button class="button" data-close>完成</button><button class="button primary" data-copy-invite>${link?'复制邀请链接':'复制邀请说明'}</button></div></div>`);
 load();
}
async function getChoices(body:Dict,appliance?:string):Promise<Choices>{const result:Choices={};if(preview)return result;const profile=actor(),shape=JSON.stringify(body)+JSON.stringify(schema(body).properties?.items?schemas.QuoteItemInputDTO:{});
 if(/technicianId|replacementTechnicianId|staffId/.test(shape)&&may('listStaffOptions',profile)){const list=await api('listStaffOptions');result.technicianId=list.map((s:Dict)=>({value:s.id,label:s.name+' · '+s.id+'（当前任务 '+s.activeTaskCount+'）'}));result.replacementTechnicianId=result.technicianId;result.staffId=result.technicianId;}
 if(shape.includes('serviceItemId')&&may('listServiceItems',profile)){const list=await optionsAll('listServiceItems');result.serviceItemId=list.filter(x=>x.status==='ENABLED'&&(!appliance||x.applianceType===appliance)).map(x=>({value:x.id,label:x.name+' · '+x.code}));}
 if(shape.includes('pricingRuleId')&&may('listPricingRules',profile)){const list=await optionsAll('listPricingRules');result.pricingRuleId=list.filter(x=>x.status==='ENABLED'&&Date.parse(x.effectiveFrom)<=Date.now()&&(!x.effectiveTo||Date.parse(x.effectiveTo)>Date.now())).map(x=>({value:x.id,label:x.title+' · '+x.clauseNo+' / '+x.ruleVersion,serviceId:x.serviceItemId,feeType:x.feeType,fixedPrice:x.fixedPrice,minPrice:x.minPrice,maxPrice:x.maxPrice}));}return result;}
function seedFor(op:string,row:Dict,detail?:Dict){const w=detail?.workOrder||row.workOrder||{},base=row.quote||row.repairRequest||row;const seed:Dict={...base,expectedVersion:base.version,expectedWorkOrderVersion:w.version,expectedQuoteVersion:base.version,expectedApprovalVersion:row.approval?.version??row.version,expectedAssignmentVersion:detail?.assignment?.version,expectedAppointmentVersion:detail?.appointmentVersion,expectedAddressVersion:row.serviceContact?.addressVersion,expectedContentHash:row.contentHash,technicianId:detail?.assignment?.technicianId??base.technicianId};
 if(op==='issueStaffInvitation')seed.expectedStaffVersion=row.version;
 if(op==='adminCreateQuote'){const history=[...(w.quoteHistory||[])].sort((a,b)=>a.versionNo-b.versionNo),accepted=history.filter(x=>x.status==='ACCEPTED').at(-1);Object.assign(seed,{submissionSource:'DISPATCHER_PROXY',serviceRecordId:detail?.serviceRecords?.filter((r:Dict)=>r.kind==='INSPECTION').at(-1)?.id,previousQuoteId:history.at(-1)?.id||null,baseQuoteId:accepted?.id||null,items:[{}]});}
 if(op==='adminUpdateQuote')seed.items=(base.items||[]).map((item:Dict)=>({...item,selectedUnitPrice:item.unitPrice||item.selectedUnitPrice}));
 if(op==='createWorkOrderAppointment'&&detail?.preferredVisit)Object.assign(seed,detail.preferredVisit,{confirmed:false});
 return seed;
}
let ephemeral:Command[]=[];
async function loadPending(){if(preview||!session){pending=[];return;}const user=session.staff.id;const list=await journal.list(user);if(session?.staff.id===user)pending=[...list,...ephemeral.filter(x=>x.owner===user)];}
function isEphemeral(op:string){return op.startsWith('staffL')||op.startsWith('staffR')||op==='changeStaffPassword';}
function resourceFor(op:string,row:Dict,detail?:Dict){if(detail?.workOrder?.id||row.workOrder?.id)return 'work-order:'+(detail?.workOrder?.id||row.workOrder.id);return operations[op].path.split('/{')[0]+':'+(row.id||row.repairRequest?.id||row.quote?.id||'new');}
async function openCommand(context:{action:Action;row:Dict;detail?:Dict;page:string}){if(preview)return;const {action,row,detail}=context;if(!may(action.op,actor()))return;
 const resource=resourceFor(action.op,row,detail);if(pending.some(c=>c.resource===resource)){showPending();return;}
 const isTriage=action.op==='manualTriageRepairRequest',isPublish=action.op==='adminPublishJobPool',isInvite=action.op===inviteAction.op,isNewInvite=action.op===newInviteAction.op;
 const returnToPool=action.action==='APPROVE_RETURN_TO_POOL';
 const returnDecision=action.op==='decideAssignmentReturn';
 const approvedReturn=action.seed?.approved===true;
 const resolutions:string[]=action.op==='adminCoordination'?detail?.allowedCoordinationResolutions||[]:[];
 if(action.op==='adminCoordination'&&!resolutions.length)return;
 const fixedResolution=resolutions.length===1?resolutions[0]:undefined;
 const continueAfterSafety=fixedResolution==='CONTINUE_AFTER_SAFETY';
 const canCancelRejected=resolutions.includes('CANCEL_REJECTED_QUOTE');
 const confirmationOnly=isTriage||isPublish||isInvite||isNewInvite||!!fixedResolution;
 const stamp=dialog(action.label,skeleton(),!confirmationOnly);try{let seed={...seedFor(action.op,row,detail),...action.seed},args={id:row.quote?.id||row.repairRequest?.id||row.id||row.workOrder?.id,...action.args};if(action.op==='confirmWorkOrderAppointment')args.id=detail?.workOrder?.appointment?.id;if(action.op==='decideAssignmentReturn')args.id=detail?.assignment?.id;if(['approveQuoteOverride','rejectQuoteOverride'].includes(action.op)&&row.approval)args.id=row.approval.id;
 pathFor(action.op,args);let bodySchema=schema(operationSchema(action.op));
 if(isTriage||isPublish||fixedResolution||returnDecision){
  const hidden=isTriage?['reviewNote','informationConfirmed','safetyReviewed']
   :fixedResolution?(continueAfterSafety?['resolution','reason']:['resolution'])
   :returnDecision?(approvedReturn?['approved']:['approved','replacementTechnicianId'])
   :returnToPool?[]:['reason','expectedAssignmentVersion'];
  bodySchema={...bodySchema,properties:Object.fromEntries(Object.entries(bodySchema.properties).filter(([name])=>!hidden.includes(name))),
   required:bodySchema.required?.filter((name:string)=>!hidden.includes(name))};
 }
 const options=await getChoices(bodySchema,detail?.workOrder?.applianceType);if(stamp!==dialogEpoch)return;choices=options;
 if(returnDecision&&approvedReturn){
  options.replacementTechnicianId=(options.replacementTechnicianId||[]).filter(choice=>choice.value!==String(detail?.assignment?.technicianId));
  if(!options.replacementTechnicianId.length){
   overlays.querySelector('.modal-body')!.innerHTML=empty('暂无其他可派维修人员','请返回工单详情，选择符合当前条件的退回处理方式。');
   return;
  }
  bodySchema={...bodySchema,required:[...bodySchema.required,'replacementTechnicianId']};
 }
 if(resolutions.length>1)options.resolution=resolutions.map(value=>({value,label:text(value)}));
 if(detail?.serviceRecords){options.serviceRecordId=detail.serviceRecords.filter((r:Dict)=>r.kind==='INSPECTION').map((r:Dict)=>({value:r.id,label:dateText(r.submittedAt)+' · '+r.summary}));options.correctsRecordId=detail.serviceRecords.filter((r:Dict)=>!action.seed?.kind||r.kind===action.seed.kind).map((r:Dict)=>({value:r.id,label:r.summary+' · '+r.id}));}
 if(action.op==='assignWorkOrder'){
  seed.technicianId='';
  const assignment=detail?.assignment;
  if(assignment&&['PENDING','ACTIVE','RETURN_REQUESTED'].includes(assignment.status))
   options.technicianId=(options.technicianId||[]).filter(choice=>choice.value!==String(assignment.technicianId));
  if(detail?.reworkTechnicianId){
   options.technicianId=(options.technicianId||[]).filter(choice=>choice.value===String(detail.reworkTechnicianId));
   seed.technicianId=options.technicianId[0]?.value||'';
  }
 }
 if(action.op==='adminUpdateQuote')seed.items=(seed.items||[]).map((item:Dict)=>({...item,serviceItemId:'',pricingRuleId:''}));
 if(action.op==='adminCreateQuote')options.submissionSource=[{value:'DISPATCHER_PROXY',label:'调度 / 管理员代录'},{value:'ADMIN_TAKEOVER',label:'管理员已接管后录入'}];
 const confirmText=isTriage?`<div class="command-confirmation"><p>${escape(label('informationConfirmed'))}</p><p>${escape(label('safetyReviewed'))}</p></div>`
  :isPublish?`<div class="command-confirmation"><p>${returnToPool?'批准退回后将结束原分派和预约，并发布到接单大厅。':'确认将工单 '+escape(detail?.workOrder?.workOrderNo)+' 发布到接单大厅吗？'}</p></div>`
  :isInvite?`<div class="command-confirmation"><p>为 ${escape(row.name)}（${escape(row.username)}）签发维修邀请。</p><p>邀请码 24 小时内有效，提交申请后仍需管理员审核。</p></div>`
  :isNewInvite?'<div class="command-confirmation"><p>签发一份 24 小时内有效的单次邀请。微信链接权限未开通时，将提供邀请码。</p><p>新人提交姓名和电话后，到「维修身份审核」确认。通过后系统会建立员工档案并开通维修工作区。</p></div>'
  :fixedResolution?`<div class="command-confirmation"><p>${continueAfterSafety?'客户确认继续维修并选定上门时段后，原工单将进入待分派。':'确认执行「'+escape(text(fixedResolution))+'」吗？'}</p></div>`:'';
 overlays.querySelector('.modal-body')!.innerHTML=`<form id="command-form">${confirmText||`<div class="form-notice">${detail?.workOrder?escape(detail.workOrder.workOrderNo)+' · ':''}${escape(action.label)}${['createPricingRules','updatePricingRules'].includes(action.op)?'：上门费统一为固定 50 元。其他费用可选固定价或区间价。':''}${['resolveUrges','resolveHumanHandoffs'].includes(action.op)?'：处理结论会显示给客户，请填写可对外说明的内容。':''}${action.op==='adminUpdateQuote'?'：请逐项重新核对服务项目和现行价格规则。':''}${action.danger?'，请核对对象和处理依据。':'，请核对资料后提交。'}</div>`}<div class="form-grid">${controls(bodySchema,seed,options)}</div><div class="form-error" role="alert"></div><div class="modal-actions"><button class="button" type="button" data-close>取消</button><button class="button primary" type="submit">确认${escape(action.label)}</button></div></form>`;
 if(canCancelRejected){
  const form=overlays.querySelector<HTMLFormElement>('#command-form')!;
  const reasonInput=form.querySelector<HTMLTextAreaElement>('[name=reason]')!;
  const resolutionInput=form.querySelector<HTMLSelectElement>('[name=resolution]');
  form.querySelector('.form-error')!.insertAdjacentHTML('beforebegin','<label class="check" data-cancel-rejected hidden><input type="checkbox">已向客户确认取消及上门费规则。请填写确认依据。</label>');
  const confirmation=form.querySelector<HTMLElement>('[data-cancel-rejected]')!;
  const checkbox=confirmation.querySelector<HTMLInputElement>('input')!;
  const sync=()=>{const cancelling=(fixedResolution||resolutionInput?.value)==='CANCEL_REJECTED_QUOTE';confirmation.hidden=!cancelling;confirmation.style.display=cancelling?'':'none';checkbox.required=cancelling;reasonInput.required=cancelling;};
  resolutionInput?.addEventListener('change',sync);
  sync();
 }
 if(action.op==='createWorkOrderAppointment'&&detail?.visitPolicy==='CUSTOMER_SLOT'){overlays.querySelectorAll<HTMLInputElement>('[name=startAt],[name=endAt]').forEach(x=>x.readOnly=true);}
 if(['createPricingRules','updatePricingRules'].includes(action.op)){
  const type=overlays.querySelector<HTMLSelectElement>('[name=feeType]')!;
  const fixed=overlays.querySelector<HTMLInputElement>('[name=fixedPrice]')!;
  const range=overlays.querySelectorAll<HTMLInputElement>('[name=minPrice],[name=maxPrice]');
  const applyVisitFee=()=>{
   const visit=type.value==='VISIT';
   if(visit)fixed.value='50.00';
   fixed.readOnly=visit;
   range.forEach(input=>{if(visit)input.value='';input.readOnly=visit;});
  };
  type.addEventListener('change',applyVisitFee);
  applyVisitFee();
 }
 let command:Command|null=null;
 overlays.querySelector<HTMLFormElement>('#command-form')!.onsubmit=async e=>{e.preventDefault();const form=e.currentTarget as HTMLFormElement;
  try{if(!command){const body=formValue(form,bodySchema);if(returnDecision)body.approved=approvedReturn;if(isTriage)Object.assign(body,{informationConfirmed:true,safetyReviewed:true});if(fixedResolution)body.resolution=fixedResolution;if(['adminCreateQuote','adminUpdateQuote'].includes(action.op))validateQuoteInputs(body,options,seed.quoteType==='ADDITIONAL');const candidate:Command={id:key(),owner:session!.staff.id,op:action.op,args,body,key:key(),createdAt:Date.now(),resource};if(isEphemeral(candidate.op))ephemeral.push(candidate);else await journal.save(candidate);command=candidate;await loadPending();}if(await executeCommand(command,form,stamp)==='INPUT_REJECTED')command=null;}catch(f){errorInDialog((f as Error).message);}
 };
 }catch(e){if(stamp===dialogEpoch)overlays.querySelector('.modal-body')!.innerHTML=empty('操作资料暂不可用',(e as Error).message);}}
const successMessages:Record<string,string>={
 manualTriageRepairRequest:'已核对，等待客户确认生成工单',
 assignWorkOrder:'已定向派单，请等待师傅接单',
 adminPublishJobPool:'已发布到接单大厅',
 confirmRepairServiceContact:'服务联系已确认',
 resolveHumanHandoffs:'已记录协调结论'
};
async function executeCommand(command:Command,form?:HTMLFormElement,stamp=dialogEpoch){if(!sameActor(command,actor()))throw Error('账号或岗位已变更，不能恢复这项操作');if(commandExpired(command))throw Error('原操作已超过恢复时限，请先核对结果，不会换键重发');const modal=overlays.querySelector<HTMLDialogElement>('dialog');if(modal?.dataset.busy==='true')return;if(modal)modal.dataset.busy='true';form?.querySelectorAll<HTMLInputElement>('input,select,textarea,button').forEach(e=>e.disabled=true);
 try{const result=await api(command.op,command.args,command.body,command.key);if(!sameActor(command,actor()))return;if(stamp!==dialogEpoch||document.visibilityState==='hidden'){await loadPending();return;}await finishCommand(command);if(modal)modal.dataset.busy='false';closeDialog();
 if(command.op==='changeStaffPassword'){forgetSession();toast('密码已修改，请使用新密码重新登录');return;}
 toast(command.op==='adminCoordination'&&command.body.resolution==='CONTINUE_AFTER_SAFETY'
  ?'等待客户确认并选择上门时段':command.op==='adminCoordination'&&command.body.resolution==='REQUOTE'
  ?'已标记重新报价，师傅可在任务详情查看':successMessages[command.op]||'操作已完成');
 if(command.op===inviteAction.op||command.op===newInviteAction.op){showInvitationReceipt(result);return;}
 await load();
 }catch(e){const unknown=unknownResult(e);if(!unknown){await finishCommand(command);if(modal)modal.dataset.busy='false';if(form&&stamp===dialogEpoch&&e instanceof ApiError&&[400,422].includes(e.status)){form.querySelectorAll<HTMLInputElement>('input,select,textarea,button').forEach(x=>x.disabled=false);errorInDialog(e.message);return 'INPUT_REJECTED';}if(stamp===dialogEpoch){const target=overlays.querySelector('.modal-body');if(target)target.innerHTML=`<div class="error-banner">${escape((e as Error).message)}${e instanceof ApiError&&e.status===409?'。记录已变化，请关闭后刷新核对，不会自动更换版本重试。':''}</div><div class="modal-actions"><button class="button" data-close>关闭并核对</button></div>`;}}
 else{if(modal)modal.dataset.busy='false';if(stamp===dialogEpoch){errorInDialog('结果待确认：'+(e as Error).message+'。恢复将使用原内容、原版本和原请求键。');form?.querySelectorAll<HTMLButtonElement>('button').forEach(e=>e.disabled=false);const submit=form?.querySelector<HTMLButtonElement>('[type=submit]');if(submit)submit.textContent='恢复原操作结果';}await loadPending();}
 }finally{if(modal)modal.dataset.busy='false';}}
async function finishCommand(command:Command){if(isEphemeral(command.op))ephemeral=ephemeral.filter(x=>x.id!==command.id);else await journal.remove(command.id);await loadPending();}
function showPending(){dialog('待核对操作',pending.length?`<p class="muted">恢复始终使用原请求，不能替换输入或版本。超过15分钟的操作仅能核对结果。</p>${pending.map((c,i)=>section(operations[c.op].summary||c.op,`<p class="muted">提交时间 ${escape(dateText(new Date(c.createdAt).toISOString()))}</p><div class="inline-actions"><button class="button primary" data-recover="${i}" ${commandExpired(c)||!sameActor(c,actor())?'disabled':''}>恢复原操作</button><button class="button" data-reconcile="${i}">已核对结果</button></div>`)).join('')}<div class="form-error"></div>`:empty('没有待恢复操作','本账号没有未决的业务提交'),true);}
function reconcile(index:number){const c=pending[index];if(!c)return;dialog('核对原操作结果',`<p>请先在对应业务详情或审计记录中核实“${escape(operations[c.op].summary)}”是否已经生效。</p><p class="muted">此处只收起本机恢复记录，不提交、不撤销后台业务，也不会重新发起原操作。</p><form id="reconcile-form"><label class="check"><input type="checkbox" required>我已核实原操作结果，确认不再恢复该请求</label><div class="modal-actions"><button class="button" data-close type="button">返回</button><button class="button primary">确认核对完成</button></div></form>`);overlays.querySelector<HTMLFormElement>('#reconcile-form')!.onsubmit=async e=>{e.preventDefault();await finishCommand(c);closeDialog();render();};}

async function enterWorkspace(){
 if(!session)return;
 epoch++;
 const allowed=visiblePages(session.staff);
 const requestedRoute=routeFromHash();
 route=allowed.some(p=>p.id===requestedRoute)?requestedRoute:(allowed[0]?.id||'services');
 rows=[];
 choices={};
 pending=[];
 logoutPending=null;
 if(session.passwordChangeRequired){render();return;}
 try{await loadPending();}catch(error){toast((error as Error).message);}
 await load();
 loadFilterChoices();
}

async function resumeWorkspace(){
 app.innerHTML=`<div class="password-screen"><div class="panel">${skeleton()}</div></div>`;
 try{
  await restoreSession();
  await enterWorkspace();
 }catch(error){
  if(error instanceof ApiError&&[401,403].includes(error.status)){login();return;}
  app.innerHTML=`<div class="password-screen"><div class="panel"><h2>暂时无法恢复会话</h2><p class="muted">${escape((error as Error).message)}</p><button class="button primary" id="resume-session">重新连接</button></div></div>`;
  app.querySelector<HTMLButtonElement>('#resume-session')!.onclick=resumeWorkspace;
 }
}

function login(){if(preview)return;closeDialog();app.innerHTML=`<div class="login-screen"><div class="login-intro"><a class="brand"><span class="brand-symbol">${icon('tool')}</span><span><b>安心修</b><small>运营管理台</small></span></a><div><span class="login-eyebrow">维修服务运营工作区</span><h1>让维修有序，<br>让交付安心。</h1><p>从客户报修到师傅上门，<br>集中处理每一项服务与协作。</p><div class="login-points"><span>${icon('shield')}权限分工</span><span>${icon('orders')}全程留痕</span><span>${icon('clock')}待办协同</span></div></div><small>FixMate · 人工业务管理</small></div><div class="login-main"><form id="login-form"><div class="login-mark">${icon('tool')}</div><h2>登录管理工作区</h2><p>请使用平台分配的员工账号登录</p><label class="field"><span>登录账号</span><input name="username" autocomplete="username" required maxlength="64" placeholder="请输入员工账号"></label><label class="field"><span>密码</span><input name="password" type="password" autocomplete="current-password" required maxlength="128" placeholder="请输入登录密码"></label><div class="form-error" role="alert"></div><button class="button primary login-submit" type="submit">登录工作区 ${icon('arrow')}</button><small>首次登录或密码重置后，请按提示修改密码。</small></form></div></div>`;
 let loginCommand:{body:Dict;key:string}|null=null,busy=false;
 const form=app.querySelector<HTMLFormElement>('#login-form')!;
 form.onsubmit=async e=>{e.preventDefault();if(busy)return;busy=true;if(!loginCommand){const values=new FormData(form);loginCommand={body:{username:String(values.get('username')).trim(),password:String(values.get('password'))},key:key()};}const submit=form.querySelector<HTMLButtonElement>('[type=submit]')!;submit.disabled=true;submit.textContent='正在登录…';
 try{const value=await raw('/__session/login','POST',loginCommand.body,loginCommand.key);setSession(value);loginCommand=null;await enterWorkspace();}
 catch(error){const unknown=unknownResult(error)&&!(error instanceof ApiError&&[401,403].includes(error.status));form.querySelector('.form-error')!.textContent=(error as Error).message+(unknown?'。登录结果待确认，请用原内容恢复。':'');form.querySelectorAll<HTMLInputElement>('input').forEach(x=>x.disabled=unknown);if(!unknown)loginCommand=null;submit.textContent=unknown?'恢复原登录结果':'登录工作区';}
 finally{busy=false;submit.disabled=false;}};
}
function passwordScreen(){app.innerHTML=`<div class="password-screen"><form id="password-form" class="panel"><div class="login-mark">${icon('shield')}</div><h2>修改初始密码</h2><p class="muted">完成修改后，请使用新密码重新登录。</p><div class="form-grid">${controls(operationSchema('changeStaffPassword'),{})}</div><div class="form-error"></div><button class="button primary" type="submit">确认修改并重新登录</button><button class="text-button" type="button" data-logout>退出登录</button></form></div>`;let command:{body:Dict;key:string;at:number}|null=null,busy=false;const form=app.querySelector<HTMLFormElement>('form')!;form.onsubmit=async e=>{e.preventDefault();if(busy)return;const submit=form.querySelector<HTMLButtonElement>('[type=submit]')!;try{if(!command)command={body:formValue(form,operationSchema('changeStaffPassword')),key:key(),at:Date.now()};if(Date.now()-command.at>15*60*1000)throw Error('原修改结果需核对，请退出并用新密码重新登录，不会重新提交');busy=true;submit.disabled=true;await api('changeStaffPassword',{},command.body,command.key);command=null;forgetSession();toast('密码已修改，请使用新密码重新登录');}catch(error){const unknown=unknownResult(error)&&!(error instanceof ApiError&&[401,403].includes(error.status));form.querySelector('.form-error')!.textContent=(error as Error).message+(unknown?'。结果待确认，恢复时保持原请求。':'');form.querySelectorAll<HTMLInputElement>('input').forEach(x=>x.disabled=unknown);if(!unknown)command=null;submit.textContent=unknown?'恢复原修改结果':'确认修改并重新登录';}finally{busy=false;submit.disabled=false;}};}

let logoutPending:{key:string;body:Dict}|null=null;
async function logout(){if(preview){location.href=location.pathname;return;}if(!session)return;if(!confirm(pending.length?'仍有操作结果待核对，加密恢复记录会保留给本账号。确认退出？':'确认退出管理工作区？'))return;logoutPending??={key:key(),body:{sessionId:session.sessionId}};try{await raw('/__session/logout','POST',logoutPending.body,logoutPending.key);logoutPending=null;forgetSession();}catch(e){if(e instanceof ApiError&&[401,403].includes(e.status)){logoutPending=null;forgetSession();}else toast('退出结果待确认，请再次点击退出以恢复原请求。');}}
async function loadFilterChoices(){const user=actor()?.id;try{if(!preview&&may('listStaffOptions',actor())){const data=await api('listStaffOptions');if(user!==actor()?.id)return;choices.technicianId=data.map((s:Dict)=>({value:s.id,label:s.name+' · '+s.id}));choices.staffId=choices.technicianId;if(!loading&&!overlays.children.length)render();}}catch{/* 筛选可稍后刷新，不能据此伪造员工资料。 */}}
document.addEventListener('click',async event=>{const el=(event.target as Element).closest<HTMLElement>('button,a');if(!el||el.hasAttribute('disabled'))return;
 if(el.dataset.nav){nav(el.dataset.nav);return;}if(el.hasAttribute('data-menu')){menuCollapsed=!menuCollapsed;render();return;}
 if(el.hasAttribute('data-refresh')){load();return;}if(el.hasAttribute('data-reset')){query={};page=1;load();return;}
 if(el.dataset.page){page=Number(el.dataset.page);load();return;}
 if(el.dataset.metric){const v=metricLinks[el.dataset.metric];nav(v.to,v.filter);return;}
 if(el.dataset.status){nav('orders',{status:el.dataset.status});return;}
 if(el.hasAttribute('data-open')){openRow(Number(el.dataset.open));return;}
 if(el.hasAttribute('data-publish-row')){openRow(Number(el.dataset.publishRow),true);return;}
 if(el.hasAttribute('data-issue-invite')){chooseInvitationRecipient();return;}
 if(el.hasAttribute('data-invite-new')){openCommand({action:newInviteAction,row:{},page:route});return;}
 if(el.hasAttribute('data-copy-invite')){
  const message=overlays.querySelector<HTMLTextAreaElement>('#invitation-message');
  if(!message)return;
  const copyLabel=el.textContent?.includes('链接')?'邀请链接':'邀请说明';
  try{await navigator.clipboard.writeText(message.value);if(message.isConnected)errorInDialog(copyLabel+'已复制');}
  catch{if(message.isConnected){message.focus();message.select();errorInDialog('请按 Ctrl+C 复制已选中的'+copyLabel);}}
  return;
 }
 if(el.hasAttribute('data-close')){event.preventDefault();closeDialog();return;}
 if(el.dataset.command){const context=contexts.get(el.dataset.command);if(context)openCommand(context);return;}
 if(el.dataset.detailTab||el.dataset.nextTab){const name=el.dataset.detailTab||el.dataset.nextTab;overlays.querySelectorAll<HTMLElement>('[data-detail-panel]').forEach(x=>x.hidden=x.dataset.detailPanel!==name);overlays.querySelectorAll('[data-detail-tab]').forEach(x=>x.classList.toggle('selected',(x as HTMLElement).dataset.detailTab===name));return;}
 if(el.dataset.relatedRepair){relatedRepair(el.dataset.relatedRepair,el.dataset.fromOrder);return;}
 if(el.dataset.relatedOrder){relatedOrder(el.dataset.relatedOrder,el.hasAttribute('data-appointment'));return;}
 if(el.dataset.assignmentHistory){if(preview){toast('样式预览不读取后台历史');return;}historyDetail(el.dataset.assignmentHistory);return;}
 if(el.dataset.historyDetail){const stamp=dialog('历史分派详情',skeleton(),true);try{const d=await api('getAssignmentHistory',{id:el.dataset.historyDetail});if(stamp===dialogEpoch)overlays.querySelector('.modal-body')!.innerHTML=detailGrid(d);}catch(e){if(stamp===dialogEpoch)overlays.querySelector('.modal-body')!.innerHTML=empty('读取失败',(e as Error).message);}return;}
 if(el.dataset.addItem){const id=el.dataset.addItem,target=overlays.querySelector<HTMLElement>(`[data-array="${id}"] .array-rows`);if(target){const indices=[...target.querySelectorAll<HTMLInputElement>('[name]')].map(e=>Number(e.name.slice(id.length+1).split('.')[0])).filter(Number.isFinite);const n=Math.max(-1,...indices)+1;target.insertAdjacentHTML('beforeend',arrayRow({$ref:'#/components/schemas/QuoteItemInputDTO'},{},choices,id,n));}return;}
 if(el.hasAttribute('data-remove-item')){el.closest('fieldset')?.remove();return;}
 if(el.hasAttribute('data-pending')){try{await loadPending();showPending();}catch(e){toast((e as Error).message);}return;}
 if(el.hasAttribute('data-recover')){const c=pending[Number(el.dataset.recover)];try{await executeCommand(c);}catch(e){errorInDialog((e as Error).message);}return;}
 if(el.hasAttribute('data-reconcile')){reconcile(Number(el.dataset.reconcile));return;}
 if(el.hasAttribute('data-password')){if(!preview)openCommand({action:{op:'changeStaffPassword',label:'修改登录密码'},row:{},page:route});return;}
 if(el.hasAttribute('data-logout'))logout();
});
document.addEventListener('submit',e=>{const form=e.target as HTMLFormElement;if(form.id==='filters'){e.preventDefault();query=Object.fromEntries(new FormData(form));page=1;load();}});
document.addEventListener('change',e=>{const el=e.target as HTMLSelectElement;if(el.name?.endsWith('.serviceItemId')){const row=el.closest('fieldset'),rule=row?.querySelector<HTMLSelectElement>('[name$=".pricingRuleId"]');if(rule){for(const option of rule.options){option.hidden=!!option.value&&option.dataset.serviceId!==el.value;option.disabled=option.hidden;}rule.value='';rule.dispatchEvent(new Event('change',{bubbles:true}));}}if(el.name?.endsWith('.pricingRuleId')){const row=el.closest('fieldset'),rule=el.selectedOptions[0],fee=row?.querySelector<HTMLSelectElement>('[name$=".feeType"]'),price=row?.querySelector<HTMLInputElement>('[name$=".selectedUnitPrice"]');if(fee)fee.value=rule?.dataset.feeType||'';if(price){price.value=rule?.dataset.fixedPrice||'';price.readOnly=!!rule?.dataset.fixedPrice;const field=price.closest('.field');field?.querySelector('.rule-hint')?.remove();if(rule?.value){const hint=document.createElement('small');hint.className='rule-hint';hint.textContent=rule.dataset.fixedPrice?'固定单价 ¥'+rule.dataset.fixedPrice:'参考区间 ¥'+rule.dataset.minPrice+' — ¥'+rule.dataset.maxPrice;field?.append(hint);}}}if(el.id==='page-size'){size=Number(el.value);page=1;load();}});
document.addEventListener('error',e=>{if(e.target instanceof HTMLImageElement){e.target.alt='照片链接暂不可用，请关闭后重新读取详情';e.target.classList.add('image-error');}},true);
window.addEventListener('session-ended',()=>{epoch++;const modal=overlays.querySelector<HTMLElement>('dialog');if(modal)modal.dataset.busy='false';closeDialog();rows=[];dashboard=null;activeDetail=null;query={};pending=[];choices={};contexts.clear();login();});
window.addEventListener('hashchange',()=>{if(actor()){const id=routeFromHash();if(pages.some(x=>x.id===id))nav(id);}});
document.addEventListener('visibilitychange',()=>{if(!actor()||preview||session?.passwordChangeRequired)return;if(document.visibilityState==='hidden'){epoch++;const modal=overlays.querySelector<HTMLElement>('dialog');if(modal)modal.dataset.busy='false';closeDialog();rows=[];total=0;dashboard=null;contexts.clear();loading=true;render();}else{load();loadPending().then(()=>{if(!overlays.children.length)render();}).catch(e=>toast((e as Error).message));}});
window.addEventListener('beforeunload',e=>{if(pending.length){e.preventDefault();}});
if(preview){route=routeFromHash()||'dashboard';if(!pages.some(x=>x.id===route))route='dashboard';load();}else resumeWorkspace();
timer=setInterval(()=>{if(actor()&&!preview&&route==='dashboard'&&!loading&&!overlays.children.length&&document.visibilityState==='visible')load();},30000);
