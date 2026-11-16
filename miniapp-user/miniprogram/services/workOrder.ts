import type {
  AcceptanceIssueDTO,
  HumanHandoffCreateDTO,
  QuoteAcceptDTO,
  QuoteRejectDTO,
  QuoteVO,
  UrgeCreateDTO,
  WorkOrderAcceptanceDTO,
  WorkOrderDetailVO,
  AppointmentRescheduleDTO,
  AppointmentVO,
  RepairRecordDetailVO
} from '@fixmate/api-contract';
import { request } from '../utils/request';

export function getWorkOrderDetail(id: string): Promise<WorkOrderDetailVO> {
  return request<WorkOrderDetailVO>({ method: 'GET', path: '/api/v1/user/work-orders/' + id });
}
export function cancelWorkOrder(id: string, expectedVersion: number, expectedVisitFee?: string): Promise<void> {
  return request<void>({ method: 'POST', path: '/api/v1/user/work-orders/' + id + '/cancel', data: { expectedVersion, expectedVisitFee } });
}

export function acceptQuote(quoteId: string, expectedQuoteVersion: number): Promise<QuoteVO> {
  const dto: QuoteAcceptDTO = { expectedQuoteVersion };
  return request<QuoteVO>({
    method: 'POST',
    path: '/api/v1/user/quotes/' + quoteId + '/accept',
    data: dto
  });
}

export function rejectQuote(quoteId: string, dto: QuoteRejectDTO): Promise<QuoteVO> {
  return request<QuoteVO>({
    method: 'POST',
    path: '/api/v1/user/quotes/' + quoteId + '/reject',
    data: dto
  });
}

export function urge(workOrderId: string, dto: UrgeCreateDTO): Promise<void> {
  return request<void>({
    method: 'POST',
    path: '/api/v1/user/work-orders/' + workOrderId + '/urges',
    data: dto
  });
}

export function requestHumanHandoff(workOrderId: string, dto: HumanHandoffCreateDTO): Promise<void> {
  return request<void>({
    method: 'POST',
    path: '/api/v1/user/work-orders/' + workOrderId + '/human-handoff',
    data: dto
  });
}

export function submitAcceptance(workOrderId: string, dto: WorkOrderAcceptanceDTO): Promise<WorkOrderDetailVO> {
  return request<WorkOrderDetailVO>({
    method: 'POST',
    path: '/api/v1/user/work-orders/' + workOrderId + '/acceptance',
    data: dto
  });
}

export function raiseAcceptanceIssue(workOrderId: string, dto: AcceptanceIssueDTO): Promise<RepairRecordDetailVO> {
  return request<RepairRecordDetailVO>({
    method: 'POST',
    path: '/api/v1/user/work-orders/' + workOrderId + '/acceptance-issues',
    data: dto
  });
}

export function rescheduleAppointment(workOrderId: string, dto: AppointmentRescheduleDTO): Promise<AppointmentVO> {
  return request<AppointmentVO>({
    method: 'POST',
    path: '/api/v1/user/work-orders/' + workOrderId + '/appointments/reschedule',
    data: dto
  });
}
