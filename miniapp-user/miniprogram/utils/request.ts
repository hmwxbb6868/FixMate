import { API_BASE_URL } from '../config/env';
import { ensureLogin, getAccessToken, refreshAccessToken } from './auth';
import { newIdempotencyKey, newRequestId } from './id';

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;
  readonly traceId: string;
  readonly data: unknown;

  constructor(code: string, message: string, status: number, traceId: string, data: unknown) {
    super(message);
    this.code = code;
    this.status = status;
    this.traceId = traceId;
    this.data = data;
  }
}

interface Envelope<T> {
  code: string;
  message: string;
  data: T;
  traceId?: string;
  timestamp?: string;
}

export interface RequestOptions {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  path: string;
  data?: unknown;
  /** 同一次用户动作的所有重试必须复用同一个键 */
  idempotencyKey?: string;
  auth?: boolean;
  /** 维修写入的预期账号；登录恢复后不允许跨账号发送原命令。 */
  expectedOwner?: string;
}

const WRITE_METHODS = ['POST', 'PUT', 'DELETE'];

/**
 * 未拿到最终结果（网络失败，或 401 后刷新失败）时保留幂等键：
 * 用户原样重试必须复用同一个键，否则后端会把重试当成新操作。
 * 拿到明确响应后释放；5xx/处理中冲突继续保留。未决键到期或容量满时拒绝新执行。
 */
interface PendingKey {
  key: string;
  at: number;
}

const pendingIdempotencyKeys: Record<string, PendingKey> = {};
const PENDING_KEY_TTL_MS = 30 * 60 * 1000;
const MAX_PENDING_KEYS = 50;

function takePendingKey(fp: string, now: number): string {
  const entry = pendingIdempotencyKeys[fp];
  if (!entry) {
    return '';
  }
  if (now - entry.at > PENDING_KEY_TTL_MS) {
    throw new ApiError('PENDING_RESULT_EXPIRED', '上次操作结果仍未确认，请联系人工核对后再操作', 0, '', null);
  }
  return entry.key;
}

function savePendingKey(fp: string, key: string, now: number): void {
  // 未决写操作不可静默淘汰；容量满时拒绝新动作，明确结果才释放。
  const keys = Object.keys(pendingIdempotencyKeys);
  if (!pendingIdempotencyKeys[fp] && keys.length >= MAX_PENDING_KEYS) {
    throw new ApiError('PENDING_REQUEST_LIMIT', '未确认的操作过多，请先重试或联系人工核对', 0, '', null);
  }
  pendingIdempotencyKeys[fp] = { key, at: now };
}

function clearPendingKey(fp: string, key: string): void {
  if (pendingIdempotencyKeys[fp]?.key === key) delete pendingIdempotencyKeys[fp];
}

function requestFingerprint(options: RequestOptions): string {
  let body = '';
  try {
    body = JSON.stringify(options.data);
  } catch (e) {
    body = String(options.data);
  }
  const owner = typeof wx.getStorageSync === 'function' ? String(wx.getStorageSync('fm.customerId') || '') : '';
  return owner + ' ' + options.method + ' ' + options.path + ' ' + (body || '');
}

function send(options: RequestOptions, idempotencyKey: string): Promise<{ status: number; body: unknown }> {
  return new Promise((resolve, reject) => {
    const header: Record<string, string> = {
      'content-type': 'application/json',
      'X-Request-Id': newRequestId()
    };
    if (options.auth !== false) {
      header.Authorization = 'Bearer ' + getAccessToken();
    }
    if (WRITE_METHODS.indexOf(options.method) >= 0) {
      header['Idempotency-Key'] = idempotencyKey;
    }
    wx.request({
      url: API_BASE_URL + options.path,
      method: options.method,
      data: options.data as WechatMiniprogram.IAnyObject,
      header,
      success(res) {
        resolve({ status: res.statusCode, body: res.data });
      },
      fail() {
        reject(new ApiError('NETWORK_ERROR', '网络不可用，请检查网络后重试', 0, '', null));
      }
    });
  });
}

function activeOwner(): string {
  return typeof wx.getStorageSync === 'function' ? String(wx.getStorageSync('fm.customerId') || '') : '';
}
function requireSameOwner(owner: string): void {
  if (owner !== activeOwner()) throw new ApiError('ACCOUNT_CHANGED', '登录身份已变化，请重新进入页面', 0, '', null);
}

export async function request<T>(options: RequestOptions): Promise<T> {
  if (options.auth !== false) {
    await ensureLogin();
  }
  const owner = activeOwner();
  if (options.expectedOwner && owner !== options.expectedOwner) throw new ApiError('ACCOUNT_CHANGED', '登录身份已变化，请重新进入页面', 0, '', null);
  const isWrite = WRITE_METHODS.indexOf(options.method) >= 0;
  const fingerprint = isWrite && !options.idempotencyKey ? requestFingerprint(options) : '';
  const now = Date.now();
  const cachedKey = fingerprint ? takePendingKey(fingerprint, now) : '';
  const idempotencyKey = options.idempotencyKey || cachedKey || newIdempotencyKey();
  if (fingerprint && !cachedKey) {
    savePendingKey(fingerprint, idempotencyKey, now);
  }

  let result: { status: number; body: unknown };
  try {
    result = await send(options, idempotencyKey);
    requireSameOwner(owner);
    if (result.status === 401 && options.auth !== false) {
      await refreshAccessToken();
      requireSameOwner(owner);
      result = await send(options, idempotencyKey);
      requireSameOwner(owner);
    }
  } catch (error) {
    // 最终结果未知：保留键，供用户原样重试
    throw error;
  }
  const responseCode = (result.body as Envelope<T> | null)?.code;
  if (fingerprint && result.status < 500 && responseCode !== 'DUPLICATE_REQUEST') {
    clearPendingKey(fingerprint, idempotencyKey);
  }

  const envelope = result.body as Envelope<T> | null;
  if (result.status >= 200 && result.status < 300 && envelope && envelope.code === 'OK') {
    return envelope.data;
  }
  if (envelope && envelope.code) {
    throw new ApiError(envelope.code, envelope.message || '请求失败', result.status,
      envelope.traceId || '', envelope.data);
  }
  throw new ApiError('UNEXPECTED_RESPONSE', '服务返回格式不正确', result.status, '', result.body);
}

/** 文件上传走 wx.uploadFile，同样需要两个请求头；401 时刷新后重试一次。 */
export function uploadFile(path: string, filePath: string, formData: Record<string, string>,
                           idempotencyKey: string): Promise<{ status: number; body: unknown }> {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: API_BASE_URL + path,
      filePath,
      name: 'file',
      formData,
      header: {
        Authorization: 'Bearer ' + getAccessToken(),
        'X-Request-Id': newRequestId(),
        'Idempotency-Key': idempotencyKey
      },
      success(res) {
        let body: unknown = res.data;
        try {
          body = JSON.parse(res.data as string);
        } catch (e) {
          // 保持原始文本，由调用方按格式错误处理
        }
        resolve({ status: res.statusCode, body });
      },
      fail() {
        reject(new ApiError('NETWORK_ERROR', '上传失败，请检查网络后重试', 0, '', null));
      }
    });
  });
}
