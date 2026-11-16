import type { FileAssetVO } from '@fixmate/api-contract';
import { ApiError, uploadFile } from '../utils/request';
import { ensureLogin, refreshAccessToken } from '../utils/auth';
import { newIdempotencyKey } from '../utils/id';

interface UploadEnvelope {
  code: string;
  message: string;
  data: FileAssetVO | null;
}

/** 上传结果未知时按“用途 + 文件路径”保留键，同一次选图重试必须复用。 */
const pendingUploadKeys: Record<string, { key: string; at: number }> = {};
const MAX_PENDING_UPLOADS = 50;
const PENDING_UPLOAD_TTL_MS = 30 * 60 * 1000;

function fingerprint(filePath: string, purpose: string): string {
  const owner = typeof wx !== 'undefined' && typeof wx.getStorageSync === 'function' ? String(wx.getStorageSync('fm.customerId') || '') : '';
  return owner + '|' + purpose + '|' + filePath;
}

/** 上传到后端，由后端校验并写入私有 OSS；返回可展示的临时签名 URL。 */
export function uploadRepairImage(filePath: string): Promise<FileAssetVO> { return uploadImage(filePath,'REPAIR_MEDIA'); }
export function uploadAvatarImage(filePath: string): Promise<FileAssetVO> { return uploadImage(filePath,'AVATAR'); }
async function uploadImage(filePath: string, purpose: 'REPAIR_MEDIA' | 'AVATAR'): Promise<FileAssetVO> {
  await ensureLogin();
  const pendingKey = fingerprint(filePath, purpose);
  const checkOwner = () => { if (fingerprint(filePath,purpose)!==pendingKey) throw new ApiError('ACCOUNT_CHANGED','登录身份已变化，请重新进入页面',0,'',null); };
  const entry = pendingUploadKeys[pendingKey];
  if (entry && Date.now() - entry.at > PENDING_UPLOAD_TTL_MS) {
    throw new ApiError('PENDING_RESULT_EXPIRED', '照片上传结果尚未确认，请联系人工核对', 0, '', null);
  }
  if (!entry && Object.keys(pendingUploadKeys).length >= MAX_PENDING_UPLOADS) {
    throw new ApiError('PENDING_REQUEST_LIMIT', '待确认照片过多，请先重试已有照片', 0, '', null);
  }
  const key = entry ? entry.key : newIdempotencyKey();
  if (!entry) pendingUploadKeys[pendingKey] = { key, at: Date.now() };

  let result: { status: number; body: unknown };
  try {
    try {
      result = await uploadFile('/api/v1/user/files', filePath, { purpose }, key);
    } catch (error) {
      if (error instanceof ApiError && error.code === 'NETWORK_ERROR') {
        // 结果未知：同键重试一次，后端会重放或继续首次执行
        checkOwner();
        result = await uploadFile('/api/v1/user/files', filePath, { purpose }, key);
      } else {
        throw error;
      }
    }
    checkOwner();
    if (result.status === 401) {
      await refreshAccessToken();
      checkOwner();
      result = await uploadFile('/api/v1/user/files', filePath, { purpose }, key);
    }
  } catch (error) {
    // 网络或刷新失败：保留键，供同一次选图重试
    throw error;
  }
  checkOwner();
  const body = result.body as UploadEnvelope;
  if (result.status < 500 && body?.code !== 'DUPLICATE_REQUEST') delete pendingUploadKeys[pendingKey];
  if (result.status >= 200 && result.status < 300 && body && body.code === 'OK' && body.data) {
    return body.data;
  }
  throw new ApiError(body && body.code ? body.code : 'UPLOAD_FAILED',
    (body && body.message) || '上传失败', result.status, '', null);
}
