import type { RepairRequestVO } from '@fixmate/api-contract';

export interface PendingSubmit { draftId: string; version: number; key: string; at: number }
export const DRAFT_KEY = 'fm.draftId';
export const PENDING_SUBMIT_KEY = 'fm.pendingSubmit';
const INTENT = 'fm.repairIntent';

export function customerId(): string { return (wx.getStorageSync('fm.customerId') as string) || ''; }
function scoped(key: string): string {
  const owner = customerId();
  if (!owner) throw new Error('请先确认当前登录身份');
  return 'fm.customer.' + owner + '.' + key;
}
export function readRecovery<T>(key: string): T | null {
  if (!customerId()) return null;
  return (wx.getStorageSync(scoped(key)) as T) || null;
}
export function writeRecovery(key: string, value: unknown): void { wx.setStorageSync(scoped(key), value); }
export function removeRecovery(key: string): void { if (customerId()) wx.removeStorageSync(scoped(key)); }
export function pendingSubmit(): PendingSubmit | null {
  const p = readRecovery<PendingSubmit>(PENDING_SUBMIT_KEY);
  return p && typeof p.draftId === 'string' && typeof p.version === 'number' && typeof p.key === 'string'
    && typeof p.at === 'number' ? p : null;
}
/** 旧全局缓存只在真实接口验证归属后迁移，不替换未决请求的键/版本。 */
export async function adoptLegacyRecovery(getRecord: (id: string) => Promise<RepairRequestVO>): Promise<void> {
  if (!customerId()) return;
  const legacyPending = wx.getStorageSync(PENDING_SUBMIT_KEY) as PendingSubmit | null;
  const legacyDraft = wx.getStorageSync(DRAFT_KEY) as string;
  for (const key of [PENDING_SUBMIT_KEY, DRAFT_KEY]) {
    if (readRecovery(key)) continue;
    const value = key === PENDING_SUBMIT_KEY ? legacyPending : legacyDraft;
    const id = key === PENDING_SUBMIT_KEY ? legacyPending?.draftId : legacyDraft;
    if (!id || typeof id !== 'string') continue;
    if (key === PENDING_SUBMIT_KEY && (!legacyPending || typeof legacyPending.key !== 'string' || typeof legacyPending.version !== 'number' || typeof legacyPending.at !== 'number')) continue;
    try {
      const record = await getRecord(id);
      if (key === PENDING_SUBMIT_KEY || record.status === 'DRAFT') writeRecovery(key, value);
      wx.removeStorageSync(key);
    } catch (error) {
      const status = (error as { status?: number }).status;
      if (status !== 403 && status !== 404) throw error;
      // 不属于当前用户/不存在：保留旧凭据但不显示给本账号，绝不新建替代记录。
    }
  }
}
export function openRepair(id: string, mode: 'draft' | 'pending'): void {
  writeRecovery(INTENT, { id, mode });
  wx.switchTab({ url: '/pages/repair/new/index' });
}
export function takeRepairIntent(): { id: string; mode: 'draft' | 'pending' } | null {
  const intent = readRecovery<{ id: string; mode: 'draft' | 'pending' }>(INTENT);
  removeRecovery(INTENT);
  return intent;
}

