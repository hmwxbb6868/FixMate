import type { RepairRecordListItemVO } from '@fixmate/api-contract';
import { pendingSubmit } from './repairRecovery';

const REPAIR: Record<string,string> = { DRAFT:'草稿', SUBMITTED:'等待平台受理', COLLECTING:'信息采集中', TRIAGED:'信息已核对', CONVERTED:'已生成工单', HIGH_RISK_BLOCKED:'等待安全人工处理', CANCELLED:'已取消', HUMAN_HANDOFF:'等待人工处理' };
const ORDER: Record<string,string> = { WAITING_MANUAL_REVIEW:'等待人工处理', WAITING_CUSTOMER_VISIT:'待选上门时段', WAITING_ASSIGNMENT:'待派单', WAITING_APPOINTMENT:'待预约', APPOINTED:'已预约', IN_INSPECTION:'检测中', WAITING_QUOTE:'待报价', WAITING_QUOTE_CONFIRMATION:'待确认报价', IN_REPAIR:'维修中', PAUSED_FOR_REQUOTE:'维修暂停，待确认追加', WAITING_ACCEPTANCE:'待验收', COMPLETED:'已完成', CANCELLED:'已取消', CLOSED_UNRESOLVED:'未解决关闭' };
const TYPES: Record<string,string> = { AIR_CONDITIONER:'空调', REFRIGERATOR:'冰箱', WASHING_MACHINE:'洗衣机', AIR_SOURCE_HEAT_PUMP:'空气能' };
const BRANDS: Record<string,string> = { HAIER:'海尔', MIDEA:'美的', SIEMENS:'西门子', GREE:'格力' };
export function statusLabel(repair: string, order?: string | null): string { return order ? ORDER[order] || '状态待核对' : REPAIR[repair] || '状态待核对'; }
export function recordStatusLabel(repair: string, order?: string | null, preferredStart?: string | Date | null): string {
  if (order !== 'WAITING_APPOINTMENT') return statusLabel(repair, order);
  if (!preferredStart) return '待确认预约';
  return new Date(preferredStart).getTime() > Date.now() ? '已选时段，待确认预约' : '上门时段已过，待重新选择';
}
export function applianceLabel(type?: string | null): string { return type ? TYPES[type] || '设备' : '设备未填写'; }
export function brandLabel(code?: string | null): string { return code ? BRANDS[code] || code : ''; }
export function displayTime(value?: string | Date | null): string {
  if (!value) return '时间未记录';
  const ms = new Date(value).getTime();
  if (!Number.isFinite(ms)) return '时间未记录';
  const d = new Date(ms + 8 * 3600000), pad = (n: number) => String(n).padStart(2,'0');
  return `${d.getUTCFullYear()}-${pad(d.getUTCMonth()+1)}-${pad(d.getUTCDate())} ${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}`;
}
export function presentRecord(item: RepairRecordListItemVO) {
  const pending = pendingSubmit();
  return { ...item, title: [applianceLabel(item.applianceType),brandLabel(item.brandCode)].filter(Boolean).join(' · '),
    timeText: displayTime(item.createdAt), summaryText: item.symptomSummary || '尚未填写故障描述',
    statusText: pending?.draftId === item.id ? '提交结果待确认' : recordStatusLabel(item.repairStatus,item.workOrderStatus),
    unresolved: pending?.draftId === item.id };
}
export function mergeRecords<T extends { id: string }>(before: T[], after: T[]): T[] {
  const result = before.slice();
  after.forEach(item => { const index=result.findIndex(old=>old.id===item.id); if(index<0) result.push(item); else result[index]=item; });
  return result;
}

