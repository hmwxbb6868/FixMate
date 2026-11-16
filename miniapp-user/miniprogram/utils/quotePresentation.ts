export const feeLabel=(value:string) => ({INSPECTION:'检测费',LABOR:'人工费',PART:'配件费',VISIT:'上门费',OTHER:'其他费用'} as Record<string,string>)[value]||value;
export const quoteState=(value:string) => ({DRAFT:'草稿',SUBMITTED:'等待客户确认',ACCEPTED:'客户已确认',REJECTED:'客户已拒绝',EXPIRED:'已过期',WITHDRAWN:'已撤回'} as Record<string,string>)[value]||'状态待核对';
export const approvalState=(value:string) => ({PENDING:'等待独立审批',APPROVED:'审批已通过',REJECTED:'审批未通过',INVALIDATED:'原审批已失效'} as Record<string,string>)[value]||'状态待核对';
/** 只规范输入精度，绝不计算行金额、合计或累计应付。 */
export function decimalInput(raw:string,scale:number,integerDigits:number,positive=false):string {
 const value=raw.trim(),pattern=new RegExp('^(?:0|[1-9][0-9]{0,'+(integerDigits-1)+'})(?:\\.[0-9]{1,'+scale+'})?$');
 if(!pattern.test(value))throw new Error(positive?'请填写大于0的数量，最多三位小数':'请填写有效单价，最多两位小数');
 const parts=value.split('.'),result=parts[0]+'.'+(parts[1]||'').padEnd(scale,'0');if(positive&&!/[1-9]/.test(result))throw new Error('数量必须大于0');return result;
}
