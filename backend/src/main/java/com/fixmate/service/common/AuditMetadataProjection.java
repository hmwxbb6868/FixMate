package com.fixmate.service.common;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 审计读取只公开结构化业务元数据；凭据、自由文本、联系方式及未知嵌套字段不透传。 */
@Component
public class AuditMetadataProjection {
    private static final Set<String> NUMBERS=Set.of("version","quoteVersion","versionNo","bindingVersion","expectedVersion","count","workload","itemRevision");
    private static final Set<String> IDS=Set.of("workOrderId","quoteId","assignmentId","appointmentId","recordId","serviceRecordId","correctsRecordId","technicianId","actualTechnicianId","actualOperator","submittedBy","requestedBy","reviewedBy","approvalId","todoId","handoffId","urgeRecordId","handoffRecordId","staffId","bindingId","assignedBy","endedBy","takeoverBy","addressId","fileId");
    private static final Set<String> ENUM_KEYS=Set.of("status","fromStatus","toStatus","role","quoteType","kind","submissionSource","resolution","source","eventType","appointmentStatus","workOrderStatus");
    private static final Set<String> ENUMS=Set.of("PENDING","HANDLED","OPEN","RESOLVED","ENABLED","DISABLED","ACTIVE","REVOKED","REJECTED","APPROVED","INVALIDATED","DRAFT","SUBMITTED","ACCEPTED","EXPIRED","WITHDRAWN","INITIAL","ADDITIONAL","TECHNICIAN_SELF","DISPATCHER_PROXY","ADMIN_TAKEOVER","INSPECTION","REPAIR","URGE","HUMAN_HANDOFF","ADMIN","AUDITOR","DISPATCHER","TECHNICIAN","KNOWLEDGE_MANAGER","ASSIGNED","DECLINED","RETURN_REQUESTED","RETURN_REJECTED","REASSIGNED","FINISHED","CONFIRMED","RESCHEDULE_REQUESTED","COMPLETED","CANCELLED","MISSED","WAITING_MANUAL_REVIEW","WAITING_CUSTOMER_VISIT","WAITING_ASSIGNMENT","WAITING_APPOINTMENT","APPOINTED","IN_INSPECTION","WAITING_QUOTE","WAITING_QUOTE_CONFIRMATION","IN_REPAIR","PAUSED_FOR_REQUOTE","WAITING_ACCEPTANCE","CLOSED_UNRESOLVED","ESCALATE_TO_MANUAL","RETURN_TO_ASSIGNMENT","CONTINUE_AFTER_SAFETY","REQUOTE","RESUME_REPAIR","ABANDON_ADDITIONAL","SCHEDULE_REWORK","USER_ACCEPTANCE_ISSUE");
    private static final Set<String> MONEY=Set.of("subtotalAmount","confirmedAmountBefore","totalAmount","payableTotalAfterAcceptance","fixedPrice","minPrice","maxPrice");
    private final ObjectMapper json;
    public AuditMetadataProjection(ObjectMapper json){this.json=json;}
    public String id(JsonNode value){if(value==null||!(value.isIntegralNumber()||value.isTextual()))return null;return idText(value.asText());}
    public String idText(String s){if(s==null)return null;try{if(s.matches("[1-9][0-9]{0,18}")&&Long.parseLong(s)>0)return s;}catch(NumberFormatException ignored){}return null;}
    public JsonNode parse(String raw){try{var node=raw==null?null:json.readTree(raw);return node!=null&&node.isObject()?node:json.createObjectNode();}catch(Exception e){return json.createObjectNode();}}
    public String reason(String before,String after){for(var raw:Arrays.asList(after,before)){var n=parse(raw);if(n.path("reasonRecorded").asBoolean(false)||n.path("handlingConclusionRecorded").asBoolean(false))return "已记录业务依据（此查询不展示原文）";for(var key:List.of("reason","reviewNote","overrideReason","reviewComment","handlingConclusion"))if(n.path(key).isTextual()&&!n.path(key).asText().isBlank())return "已记录业务依据（此查询不展示原文）";}return null;}
    public String summary(String raw){
        var source=parse(raw);var result=new TreeMap<String,Object>();source.fields().forEachRemaining(entry->{var key=entry.getKey();var value=entry.getValue();
            if(IDS.contains(key)){var id=id(value);if(id!=null)result.put(key,id);}
            else if(NUMBERS.contains(key)&&value.isIntegralNumber()&&value.canConvertToLong()&&value.longValue()>=0)result.put(key,value.longValue());
            else if(ENUM_KEYS.contains(key)&&value.isTextual()&&ENUMS.contains(value.asText()))result.put(key,value.asText());
            else if(MONEY.contains(key)&&value.isTextual()&&value.asText().matches("[0-9]{1,10}\\.[0-9]{2}"))result.put(key,value.asText());
            else if(Set.of("contentHash","quoteContentHash").contains(key)&&value.isTextual()&&value.asText().matches("[a-f0-9]{64}"))result.put(key,value.asText());
            else if(Set.of("reasonRecorded","handlingConclusionRecorded","informationConfirmed","safetyReviewed").contains(key)&&value.isBoolean())result.put(key,value.booleanValue());
            else if(Set.of("startAt","endAt","scheduledStartAt","scheduledEndAt","acceptedAt","endedAt","submittedAt","reviewedAt","handledAt","effectiveFrom","effectiveTo","takeoverAt").contains(key)&&value.isTextual()){
                try{result.put(key,java.time.OffsetDateTime.parse(value.asText()).toString());}catch(java.time.format.DateTimeParseException ignored){}
            }
            else if("staff".equals(key)&&value.isObject()){
                var staff=new TreeMap<String,Object>();for(var field:List.of("role","status"))if(value.path(field).isTextual()&&ENUMS.contains(value.path(field).asText()))staff.put(field,value.path(field).asText());
                var staffId=id(value.get("id"));if(staffId!=null)staff.put("id",staffId);if(value.path("version").isIntegralNumber()&&value.path("version").canConvertToLong()&&value.path("version").longValue()>=0)staff.put("version",value.path("version").longValue());if(!staff.isEmpty())result.put(key,staff);
            }
            else if("acceptedQuoteIds".equals(key)&&value.isArray()){var ids=new ArrayList<String>();for(var v:value){var id=id(v);if(id!=null)ids.add(id);}if(ids.size()<=100)result.put(key,ids);}
        });
        if(result.isEmpty())return null;try{String text=json.writeValueAsString(result);return text.length()<=4000?text:"可公开审计元数据超出单次摘要范围";}catch(Exception e){throw new IllegalStateException("审计投影失败",e);}
    }
    public String proxy(String before,String after){for(var raw:Arrays.asList(after,before)){var node=parse(raw);for(var key:List.of("actualTechnicianId","technicianId")){var id=id(node.get(key));if(id!=null)return id;}}return null;}
}
