import fs from 'node:fs';

const target = process.argv[2] || 'release';
if (target !== 'trial' && target !== 'release') {
  console.error('usage: npm run check:publish -- trial|release');
  process.exit(2);
}

const source = fs.readFileSync(new URL('../miniprogram/config/env.ts', import.meta.url), 'utf8');
const constant = target === 'trial' ? 'TRIAL_BASE_URL' : 'RELEASE_BASE_URL';
const match = new RegExp("const\\s+" + constant + "\\s*=\\s*'([^']+)'").exec(source);
if (!match) {
  console.error(constant + ' not found in miniprogram/config/env.ts');
  process.exit(1);
}

const value = match[1];
function fail(message) {
  console.error('publish check failed: ' + message);
  process.exit(1);
}

if (value.indexOf('https://') !== 0) {
  fail(constant + ' must use https');
}
const bracketed = /^https:\/\/(\[[^\]]+\])/i.exec(value);
const hostMatch = bracketed || /^https:\/\/([^/:?#]+)/i.exec(value);
const host = hostMatch ? hostMatch[1].toLowerCase() : '';
if (!host) {
  fail(constant + ' has no valid host');
}
if (host === 'localhost' || host === '0.0.0.0' || host === '[::1]' || host === '::1'
    || host.indexOf('127.') === 0) {
  fail(constant + ' must not use a loopback address');
}
if (host === 'invalid' || host.slice(-'.invalid'.length) === '.invalid') {
  fail(constant + ' still uses a placeholder domain');
}

console.log(target + ' publish check passed: ' + value);
