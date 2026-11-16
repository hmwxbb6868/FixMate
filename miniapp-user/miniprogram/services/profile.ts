import type { CurrentUserVO } from '@fixmate/api-contract';
import { request } from '../utils/request';
import { getAccessToken } from '../utils/auth';
let active: { token: string; promise: Promise<CurrentUserVO> } | null = null;
export function getCurrentProfile(): Promise<CurrentUserVO> {
  const token=getAccessToken();
  if(active && active.token===token)return active.promise;
  const promise=request<CurrentUserVO>({method:'GET',path:'/api/v1/user/me'}).then(profile=>{
    wx.setStorageSync('fm.customerId',profile.id);return profile;
  }).finally(()=>{if(active?.promise===promise)active=null;});
  active={token,promise};return promise;
}

export async function updateCurrentProfile(dto: import('@fixmate/api-contract').UpdateCurrentUserDTO, key: string): Promise<CurrentUserVO> {
  const saved = await request<CurrentUserVO>({method:'PUT',path:'/api/v1/user/me',data:dto,idempotencyKey:key});
  // PUT的幂等重放可能带旧短签，确认保存成功后刷新签名；读取失败不把已成功写入变成未知保存。
  active = null;
  try { return await getCurrentProfile(); } catch { return saved; }
}
