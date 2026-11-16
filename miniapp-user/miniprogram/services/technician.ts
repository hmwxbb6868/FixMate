import type { JobPoolEntryVO, JobPoolEntryVOPageVO, WorkspaceAccessVO, StaffBindingVO, StaffBindingApplyDTO, StaffTaskListItemVO, StaffTaskListItemVOPageVO, StaffParticipationVO, StaffParticipationVOPageVO, StaffWorkOrderDetailVO, StaffQuoteVO, ServiceItemStaffVOPageVO, PricingRuleStaffVOPageVO } from '@fixmate/api-contract';
import { request } from '../utils/request';
/** wx.request直接接收JSON；生成Client中的Date/Set在此按传输形态使用。 */
export type Wire<T> = T extends Date ? string : T extends Set<infer V> ? Wire<V>[] : T extends Array<infer V> ? Wire<V>[] : T extends object ? { [K in keyof T]: Wire<T[K]> } : T;
export type WorkspaceAccess = Wire<WorkspaceAccessVO>;
export type Binding = Wire<StaffBindingVO>;
export type Task = Wire<StaffTaskListItemVO>;
export type Participation = Wire<StaffParticipationVO>;
export type StaffDetail = Wire<StaffWorkOrderDetailVO>;
export type StaffQuote = Wire<StaffQuoteVO>;
const base='/api/v1/technician';
export const fetchWorkspace = () => request<WorkspaceAccess>({method:'GET',path:'/api/v1/user/workspace-access'});
export async function currentBinding():Promise<Binding|null> {
  try {return await request<Binding>({method:'GET',path:'/api/v1/user/staff-bindings/current'});}
  catch(error){const e=error as {status?:number;code?:string;message?:string};if(e.status===404&&e.code==='RESOURCE_NOT_FOUND'&&e.message==='暂无员工绑定申请')return null;throw error;}
}
export const applyBinding = (dto:StaffBindingApplyDTO,key:string) => request<Binding>({method:'POST',path:'/api/v1/user/staff-bindings',data:dto,idempotencyKey:key});
export const listTasks = (page=1,size=20) => request<Wire<StaffTaskListItemVOPageVO>>({method:'GET',path:base+'/tasks',data:{page,size}});
export async function allTasks():Promise<Task[]> {
  const rows=new Map<string,Task>();let page=1,total=0;
  do { const result=await listTasks(page,100);result.items.forEach(row=>rows.set(row.id,row));total=result.total;if(!result.items.length&&page*100<total)throw new Error('任务列表正在变化，请刷新重试');page++; } while((page-1)*100<total);
  return Array.from(rows.values());
}
export const getTask = (id:string) => request<Task>({method:'GET',path:base+'/tasks/'+encodeURIComponent(id)});
export const listParticipations = (page=1) => request<Wire<StaffParticipationVOPageVO>>({method:'GET',path:base+'/participations',data:{page,size:20}});
export const getParticipation = (id:string) => request<Participation>({method:'GET',path:base+'/participations/'+encodeURIComponent(id)});
export const getStaffDetail = (id:string) => request<StaffDetail>({method:'GET',path:base+'/work-orders/'+encodeURIComponent(id)});
export const getQuotes = (id:string) => request<StaffQuote[]>({method:'GET',path:base+'/work-orders/'+encodeURIComponent(id)+'/quotes'});
export const serviceItems = (applianceType?:string,page=1) => request<Wire<ServiceItemStaffVOPageVO>>({method:'GET',path:base+'/service-items',data:{applianceType,page,size:100}});
export const pricingRules = (serviceItemId:string,page=1) => request<Wire<PricingRuleStaffVOPageVO>>({method:'GET',path:base+'/pricing-rules',data:{serviceItemId,page,size:100}});

export type PoolEntry=Wire<JobPoolEntryVO>;
export const listJobPool=(page=1)=>request<Wire<JobPoolEntryVOPageVO>>({method:"GET",path:base+"/job-pool",data:{page,size:20}});
export const getJobPool=(id:string)=>request<PoolEntry>({method:"GET",path:base+"/job-pool/"+encodeURIComponent(id)});
