import type {
  CancelRequestDTO,
  RepairDraftUpsertDTO,
  RepairRequestVO,
  UpdateRepairDraftRequest
} from '@fixmate/api-contract';
import { request } from '../utils/request';

export function createDraft(dto: RepairDraftUpsertDTO): Promise<RepairRequestVO> {
  return request<RepairRequestVO>({
    method: 'POST',
    path: '/api/v1/user/repair-requests/drafts',
    data: dto
  });
}

export function updateDraft(id: string, dto: UpdateRepairDraftRequest): Promise<RepairRequestVO> {
  return request<RepairRequestVO>({
    method: 'PUT',
    path: '/api/v1/user/repair-requests/drafts/' + id,
    data: dto
  });
}

export function getRepairRequest(id: string): Promise<RepairRequestVO> {
  return request<RepairRequestVO>({ method: 'GET', path: '/api/v1/user/repair-requests/' + id });
}

/** 命中硬规则时抛出 ApiError(HARD_RULE_BLOCKED)，data 为 SafetyBlockVO。 */
export function submitRepairRequest(id: string, expectedVersion: number, idempotencyKey?: string): Promise<RepairRequestVO> {
  return request<RepairRequestVO>({
    method: 'POST',
    path: '/api/v1/user/repair-requests/' + id + '/submit',
    data: { expectedVersion },
    idempotencyKey
  });
}

export function cancelRepairRequest(id: string, dto: CancelRequestDTO): Promise<RepairRequestVO> {
  return request<RepairRequestVO>({
    method: 'POST',
    path: '/api/v1/user/repair-requests/' + id + '/cancel',
    data: dto
  });
}


export function listRepairRecords(page = 1, group = 'ALL'): Promise<import('@fixmate/api-contract').RepairRecordListItemVOPageVO> {
  return request({ method: 'GET', path: '/api/v1/user/repair-requests', data: { page, size: 20, group } });
}
export function getRepairRecord(id: string): Promise<import('@fixmate/api-contract').RepairRecordDetailVO> {
  return request({ method: 'GET', path: '/api/v1/user/repair-records/' + encodeURIComponent(id) });
}

/** 阶段2用户本人确认；恢复始终使用原版本/键，并约束调用时的账号。 */
export function convertRepairToWorkOrder(id:string,expectedVersion:number,key:string,owner:string):Promise<import('@fixmate/api-contract').WorkOrderDetailVO> {
  return request({method:'POST',path:'/api/v1/user/repair-requests/'+encodeURIComponent(id)+'/convert-to-work-order',data:{expectedVersion},idempotencyKey:key,expectedOwner:owner});
}

export type PreferredVisitResult=import('./technician').Wire<import('@fixmate/api-contract').PreferredVisitResultVO>;
export const getPreferredVisit=(id:string)=>request<PreferredVisitResult>({method:'GET',path:'/api/v1/user/work-orders/'+encodeURIComponent(id)+'/preferred-visit'});
export const selectPreferredVisit=(id:string,body:import('./technician').Wire<import('@fixmate/api-contract').PreferredVisitDTO>,key:string,owner:string)=>request<PreferredVisitResult>({method:'POST',path:'/api/v1/user/work-orders/'+encodeURIComponent(id)+'/preferred-visit',data:body,idempotencyKey:key,expectedOwner:owner});
