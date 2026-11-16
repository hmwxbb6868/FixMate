import { displayTime } from './recordPresentation';
export interface VisitWindow {startAt:string;endAt:string}
export function visitText(slot:VisitWindow|null|undefined):string {if(!slot)return '待用户选择上门时段';const start=displayTime(slot.startAt),end=displayTime(slot.endAt);return start+' — '+(start.slice(0,10)===end.slice(0,10)?end.slice(11):end);}
export function sameVisit(a:VisitWindow|null|undefined,b:VisitWindow|null|undefined):boolean{return !!a&&!!b&&Date.parse(a.startAt)===Date.parse(b.startAt)&&Date.parse(a.endAt)===Date.parse(b.endAt);}
