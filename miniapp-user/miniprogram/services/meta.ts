import type { RepairFormMetaVO } from '@fixmate/api-contract';
import { request } from '../utils/request';

export function getRepairFormMeta(): Promise<RepairFormMetaVO> {
  return request<RepairFormMetaVO>({ method: 'GET', path: '/api/v1/user/repair-form-meta' });
}
