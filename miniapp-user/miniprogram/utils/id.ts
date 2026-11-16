const ALPHABET = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';

function randomString(length: number): string {
  let out = '';
  for (let i = 0; i < length; i++) {
    out += ALPHABET.charAt(Math.floor(Math.random() * ALPHABET.length));
  }
  return out;
}

/** 契约要求 8–64 位 [A-Za-z0-9_-]；一次用户动作复用同一个幂等键。 */
export function newRequestId(): string {
  return 'req-' + Date.now().toString(36) + randomString(10);
}

export function newIdempotencyKey(): string {
  return 'idem-' + Date.now().toString(36) + randomString(14);
}
