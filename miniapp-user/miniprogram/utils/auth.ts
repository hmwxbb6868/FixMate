import type { TokenVO } from '@fixmate/api-contract';
import { API_BASE_URL } from '../config/env';
import { newIdempotencyKey, newRequestId } from './id';

const ACCESS_KEY = 'fm.accessToken';
const REFRESH_KEY = 'fm.refreshToken';

export function getAccessToken(): string {
  return (wx.getStorageSync(ACCESS_KEY) as string) || '';
}

export function getRefreshToken(): string {
  return (wx.getStorageSync(REFRESH_KEY) as string) || '';
}

export function saveTokens(token: TokenVO): void {
  wx.setStorageSync('fm.customerId', token.user.id);
  wx.setStorageSync(ACCESS_KEY, token.accessToken);
  wx.setStorageSync(REFRESH_KEY, token.refreshToken);
}

export function clearTokens(): void {
  wx.removeStorageSync('fm.customerId');
  wx.removeStorageSync(ACCESS_KEY);
  wx.removeStorageSync(REFRESH_KEY);
}

function post(path: string, data: unknown): Promise<TokenVO> {
  return new Promise<TokenVO>((resolve, reject) => {
    wx.request({
      url: API_BASE_URL + path,
      method: 'POST',
      data: data as WechatMiniprogram.IAnyObject,
      header: {
        'content-type': 'application/json',
        'X-Request-Id': newRequestId(),
        'Idempotency-Key': newIdempotencyKey()
      },
      success(res) {
        const body = res.data as { code?: string; message?: string; data?: TokenVO };
        if (res.statusCode >= 200 && res.statusCode < 300 && body && body.data) {
          resolve(body.data);
          return;
        }
        reject(new Error((body && body.message) || '登录失败'));
      },
      fail() {
        reject(new Error('网络不可用'));
      }
    });
  });
}

let loginPromise: Promise<void> | null = null;

/** 静默登录：已有 access token 直接返回；并发调用只发一次请求。 */
export function ensureLogin(): Promise<void> {
  if (getAccessToken()) {
    return Promise.resolve();
  }
  if (loginPromise) {
    return loginPromise;
  }
  loginPromise = new Promise<void>((resolve, reject) => {
    wx.login({
      success(res) {
        if (!res.code) {
          reject(new Error('微信登录失败'));
          return;
        }
        post('/api/v1/user/auth/wx-login', { code: res.code })
          .then((token) => {
            saveTokens(token);
            resolve();
          })
          .catch(reject);
      },
      fail() {
        reject(new Error('微信登录失败'));
      }
    });
  }).finally(() => {
    loginPromise = null;
  });
  return loginPromise;
}

let refreshPromise: Promise<void> | null = null;

/** 刷新令牌：并发 401 只刷新一次，失败则清空登录态。 */
export function refreshAccessToken(): Promise<void> {
  if (refreshPromise) {
    return refreshPromise;
  }
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return Promise.reject(new Error('登录已失效'));
  }
  refreshPromise = post('/api/v1/user/auth/refresh', { refreshToken })
    .then((token) => {
      saveTokens(token);
    })
    .catch((error) => {
      clearTokens();
      throw error;
    })
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}
