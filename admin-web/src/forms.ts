import {Dict,escape,schema,operations,schemas} from './core.js';
import {label,text} from './presentation.js';
export type Choices=Record<string,Array<{value:string;label:string;serviceId?:string;feeType?:string;fixedPrice?:string|null;minPrice?:string|null;maxPrice?:string|null}>>;
export function decimalInput(raw:string,scale:number,digits:number,positive=false){const value=raw.trim(),pattern=new RegExp('^(?:0|[1-9][0-9]{0,'+(digits-1)+'})(?:\\.[0-9]{1,'+scale+'})?$');if(!pattern.test(value))throw Error(positive?'数量格式不正确，最多三位小数':'金额格式不正确，最多两位小数');const [whole,part='']=value.split('.'),normalized=whole+'.'+part.padEnd(scale,'0');if(positive&&!/[1-9]/.test(normalized))throw Error('数量必须大于0');return normalized;}
export function dateInput(value:any){if(!value)return '';const d=new Date(value);return Number.isNaN(d.valueOf())?'':new Date(d.getTime()+8*3600000).toISOString().slice(0,16);}
const longFields=new Set(['reason','facts','summary','reviewNote','reviewComment','overrideReason','description','refundPolicy','changeReason','note','partSourceNote','additionReason','pricingReason']);
export function controls(bodySchema:Dict,seed:Dict,choices:Choices={},prefix=''):string{
 const def=schema(bodySchema);return Object.entries<Dict>(def.properties||{}).map(([name,raw])=>{
  const s=schema(raw),value=seed[name],required=def.required?.includes(name),id=prefix+name;
  if(name.startsWith('expected')){if(value==null&&required)throw Error('当前记录缺少操作所需版本，请关闭后重新读取详情');return value==null?'':`<input type="hidden" name="${escape(id)}" value="${escape(value)}">`;}
  if(s.type==='array')return `<section class="form-array span-two" data-array="${escape(id)}"><div class="section-title">${escape(label(name))}<button type="button" class="button small" data-add-item="${escape(id)}">＋ 添加项目</button></div><div class="array-rows">${(Array.isArray(value)&&value.length?value:[{}]).map((row,i)=>arrayRow(raw.items,row,choices,id,i)).join('')}</div><p class="help">金额由服务器计算；区间外价格须填写依据并走独立审批。</p></section>`;
  const attrs=`name="${escape(id)}" ${required&&s.type!=='boolean'?'required':''} ${s.maxLength?'maxlength="'+s.maxLength+'"':''} ${s.minLength?'minlength="'+s.minLength+'"':''}`;
  let input='';
  if(s.type==='boolean')input=`<label class="check"><input type="checkbox" ${attrs} ${value===true?'checked':''} ${s.enum?.includes(true)?'required':''}>${escape(label(name))}</label>`;
  else if(s.enum||choices[name]){const options=choices[name]||s.enum.filter((v:any)=>v!=null&&(!['quoteType','kind'].includes(name)||!value||v===value)).map((v:any)=>({value:String(v),label:text(v)}));input=`<select ${attrs}><option value="">请选择${escape(label(name))}</option>${options.map((x:any)=>`<option value="${escape(x.value)}" ${x.serviceId?'data-service-id="'+escape(x.serviceId)+'" data-fee-type="'+escape(x.feeType)+'" data-fixed-price="'+escape(x.fixedPrice)+'" data-min-price="'+escape(x.minPrice)+'" data-max-price="'+escape(x.maxPrice)+'"':''} ${String(value)===x.value?'selected':''}>${escape(x.label)}</option>`).join('')}</select>`;}
  else if(longFields.has(name))input=`<textarea ${attrs} rows="${name==='facts'?5:3}" placeholder="请填写${escape(label(name))}">${escape(value)}</textarea>`;
  else{const type=s.format==='password'||/Password$/.test(name)||name==='password'?'password':s.format==='date-time'?'datetime-local':s.type==='integer'?'number':'text';input=`<input type="${type}" ${attrs} value="${escape(type==='datetime-local'?dateInput(value):value)}" ${s.minimum!=null?'min="'+s.minimum+'"':''} ${s.maximum!=null?'max="'+s.maximum+'"':''} ${s.type==='integer'?'step="1"':''} ${type==='password'?'autocomplete="new-password"':''} placeholder="${s.format==='date-time'?'北京时间':escape(label(name))}">`;}
  return `<div class="field ${longFields.has(name)||s.type==='boolean'?'span-two':''}">${s.type==='boolean'?'':`<label>${escape(label(name))}${required?'<b class="required">*</b>':name==='reason'?'（选填）':''}</label>`}${input}${s.format==='date-time'?'<small>北京时间（UTC+8）</small>':''}${name==='applicantContactPhone'?'<small>申请人自报电话，须人工核实</small>':''}</div>`;
 }).join('');
}
export function arrayRow(itemSchema:Dict,row:Dict,choices:Choices,prefix:string,index:number){return `<fieldset class="quote-item"><legend>费用项目 <button class="text-button danger" type="button" data-remove-item>移除</button></legend><div class="form-grid">${controls(itemSchema,row,choices,prefix+'.'+index+'.')}</div></fieldset>`;}
export function formValue(form:HTMLFormElement,def:Dict,prefix=''):Dict{
 const result:Dict={};const shape=schema(def);
 for(const[name,raw] of Object.entries<Dict>(shape.properties||{})){
  const s=schema(raw),id=prefix+name,required=shape.required?.includes(name);
  if(s.type==='array'){
   const rows=[...form.querySelectorAll<HTMLElement>('[data-array]')].find(x=>x.dataset.array===id)?.querySelectorAll('.array-rows>.quote-item');
   const indices=rows?[...rows].map(row=>(row.querySelector('[name]') as HTMLInputElement).name.slice(id.length+1).split('.')[0]):[];
   if(indices.length<(s.minItems||0))throw Error(label(name)+'至少填写'+s.minItems+'项');if(indices.length>(s.maxItems||100))throw Error(label(name)+'数量超过上限');
   result[name]=indices.map(i=>formValue(form,s.items,id+'.'+i+'.'));continue;
  }
  const el=form.elements.namedItem(id) as HTMLInputElement|null;if(!el){if(required)throw Error('缺少'+label(name)+'，请重新读取记录');continue;}
  let value:any=s.type==='boolean'?el.checked:el.value.trim();
  if(s.type==='boolean'){if(s.enum?.includes(true)&&!value)throw Error('请先确认'+label(name));}
  else if(value===''){if(required&&!s.nullable)throw Error('请填写'+label(name));if(s.nullable)result[name]=null;continue;}
  else if(s.type==='integer'){value=Number(value);if(!Number.isSafeInteger(value)||(s.minimum!=null&&value<s.minimum))throw Error(label(name)+'不正确');}
  else if(s.format==='date-time'){value=value+':00+08:00';if(Number.isNaN(Date.parse(value)))throw Error(label(name)+'不正确');}
  else if(['fixedPrice','minPrice','maxPrice','selectedUnitPrice'].includes(name)){value=decimalInput(value,2,10);}
  else if(name==='quantity'){value=decimalInput(value,3,7,true);}
  else if(/Password$/.test(name)&&name!=='currentPassword'&&!/^[0-9]{6}$/.test(value))throw Error(label(name)+'必须为六位数字');
  result[name]=value;
 }
 return result;
}
export function operationSchema(op:string){return operations[op]?.body||{type:'object',properties:{}};}
export function validateQuoteInputs(body:Dict,choices:Choices,additional:boolean){
 if(!body.expiresAt||Date.parse(body.expiresAt)<=Date.now())throw Error('报价有效期必须晚于当前时间');
 if(additional&&(!body.changeReason||body.changeReason.length<2))throw Error('请填写追加报价的变更原因');
 for(const [index,item]of body.items.entries()){
  const rule=choices.pricingRuleId?.find(r=>r.value===item.pricingRuleId);if(!rule||rule.serviceId!==item.serviceItemId||rule.feeType!==item.feeType)throw Error('第'+(index+1)+'项服务项目、价格规则和费用类型不匹配，请重新选择');
  if(!rule.fixedPrice&&!item.selectedUnitPrice)throw Error('第'+(index+1)+'项请填写选定单价');
  if((!rule.fixedPrice||item.feeType==='OTHER')&&(!item.pricingReason||item.pricingReason.length<2))throw Error('第'+(index+1)+'项请填写选价依据');
  if(additional&&(!item.additionReason||item.additionReason.length<2))throw Error('第'+(index+1)+'项请填写追加原因');
  if(item.feeType==='PART'&&(!item.itemName||!item.specification||!item.partSourceNote))throw Error('第'+(index+1)+'项配件请填写名称、规格和来源');
 }
}
