/**
 * 环境与地址。
 *
 * develop 使用本机地址；trial / release 必须使用已备案的 HTTPS 域名。
 * 发布检查会拒绝 http、回环地址和 .invalid 占位域名，避免把调试地址带进体验版或正式版
 * （冻结规则 §2.2、§16.2）。
 */
type EnvVersion = 'develop' | 'trial' | 'release';

function envVersion(): EnvVersion {
  try {
    return wx.getAccountInfoSync().miniProgram.envVersion;
  } catch (e) {
    return 'develop';
  }
}

/**
 * 开发期接口地址。
 *
 * 真机调试时必须填电脑的局域网 IP（手机访问不到 127.0.0.1）；
 * 模拟器用同一个地址也能访问。电脑换网络后 IP 会变，用 ipconfig 重新查。
 */
const LOCAL_BASE_URL = 'http://127.0.0.1:8080';
// TODO(发布前): 替换为已备案的体验版 / 正式版 HTTPS 域名
const TRIAL_BASE_URL = 'https://api.fixmate.invalid';
const RELEASE_BASE_URL = 'https://api.fixmate.invalid';

export function assertPublishableUrl(name: string, value: string): void {
  if (value.indexOf('https://') !== 0) {
    throw new Error(name + ' must use https');
  }
  const bracketed = /^https:\/\/(\[[^\]]+\])/i.exec(value);
  const match = bracketed || /^https:\/\/([^/:?#]+)/i.exec(value);
  const host = match ? match[1].toLowerCase() : '';
  if (!host) {
    throw new Error(name + ' has no valid host');
  }
  const loopback = host === 'localhost' || host === '0.0.0.0'
    || host === '[::1]' || host === '::1' || host.indexOf('127.') === 0;
  if (loopback) {
    throw new Error(name + ' must not use a loopback address');
  }
  if (host === 'invalid' || host.slice(-'.invalid'.length) === '.invalid') {
    throw new Error(name + ' still uses a placeholder domain');
  }
}

export const ENV_VERSION: EnvVersion = envVersion();
export const IS_RELEASE = ENV_VERSION === 'release';

export const API_BASE_URL = ENV_VERSION === 'develop'
  ? LOCAL_BASE_URL
  : ENV_VERSION === 'trial' ? TRIAL_BASE_URL : RELEASE_BASE_URL;

if (ENV_VERSION !== 'develop') {
  assertPublishableUrl(ENV_VERSION + ' api base url', API_BASE_URL);
}
