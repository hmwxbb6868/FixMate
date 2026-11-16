import {existsSync,mkdirSync,copyFileSync,readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {resolve,dirname} from 'node:path';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const own=resolve(root,'node_modules/typescript/lib/tsc.js'), shared=resolve(root,'../miniapp-user/node_modules/typescript/lib/tsc.js');
const compiler=existsSync(own)?own:shared;
if(!existsSync(compiler)){console.error('请在admin-web执行 npm install，再运行 npm run build');process.exit(1);}
const contractHash=createHash('sha256').update(readFileSync(resolve(root,'../docs/api/openapi-v1.yaml'))).digest('hex');
if(!readFileSync(resolve(root,'src/contract.ts'),'utf8').includes(contractHash)){console.error('管理端契约目录已过期，请先运行 scripts/generate-contract.py 并同步既有Client');process.exit(1);}
if(!existsSync(resolve(root,'../packages/api-contract/typescript/dist/index.d.ts'))){
 const client=spawnSync(process.execPath,[compiler,'-p',resolve(root,'../packages/api-contract/typescript/tsconfig.json')],{stdio:'inherit'});
 if(client.status!==0)process.exit(client.status||1);
}
const check=process.argv.includes('--check');
const result=spawnSync(process.execPath,[compiler,'-p',resolve(root,'tsconfig.json'),...(check?['--noEmit']:[])],{stdio:'inherit'});
if(result.status!==0)process.exit(result.status||1);
if(!check){mkdirSync(resolve(root,'dist/assets'),{recursive:true});copyFileSync(resolve(root,'index.html'),resolve(root,'dist/index.html'));copyFileSync(resolve(root,'src/style.css'),resolve(root,'dist/assets/style.css'));}
console.log(check?'Typecheck PASS':'Build PASS → admin-web/dist');
