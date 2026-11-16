package com.fixmate.service.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.entity.RepairRequestEntity;
import com.fixmate.entity.WorkOrderEntity;
import org.springframework.stereotype.Service;

/** 待接摘要不能通过故障自由文本旁路泄漏已知联系人、电话或详细地址。原文保持不变。 */
@Service
public class TaskSummaryProjection {
    private final ObjectMapper json;
    private final ServiceContactProjection contacts;
    public TaskSummaryProjection(ObjectMapper json,ServiceContactProjection contacts){this.json=json;this.contacts=contacts;}
    public String summary(WorkOrderEntity order,RepairRequestEntity request) {
        String text=order.getSymptomDescription();if(text==null)return "用户未填写故障摘要";
        return redact(text, order, request, 120, "具体故障接单后查看");
    }
    public String reworkText(String text,WorkOrderEntity order,RepairRequestEntity request) {
        if(text==null||text.isBlank())return null;
        if(request!=null)text=contacts.redactPersonalText(text,request);
        text=text.replaceAll("[0-9]{3}\\*{4}[0-9]{4}","[联系电话已隐藏]");
        return redact(text, order, request, 300, "部分内容涉及联系信息");
    }
    private String redact(String text,WorkOrderEntity order,RepairRequestEntity request,int limit,String hidden) {
        if(request!=null) {
            var contact=contacts.view(request,true);
            if(contact.contactPhone()!=null)text=text.replace(contact.contactPhone(),"[联系电话已隐藏]");
            if(contact.contactName()!=null&&!contact.contactName().isBlank())text=text.replace(contact.contactName(),"[联系人已隐藏]");
        }
        try {
            if(order.getAddressSnapshot()!=null) {
                var address=json.readTree(order.getAddressSnapshot());
                if(address!=null&&address.path("detail").isTextual()&&!address.path("detail").asText().isBlank())
                    text=text.replace(address.path("detail").asText(),"[服务地址已隐藏]");
            }
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){return hidden;}
        text=text.replaceAll("[0-9][0-9 ()+-]{4,}[0-9]","[联系信息已隐藏]");
        // 对疑似地址的自由文本使用真实的隐藏提示，宁可少显示摘要也不从中猜行政区域。
        if(text.matches("(?s).*(地址|家住|小区|路|街|巷|弄|门牌|单元|[0-9一二三四五六七八九十]+[号栋幢楼室]).*"))return hidden;
        return text.codePointCount(0,text.length())>limit?text.substring(0,text.offsetByCodePoints(0,limit)):text;
    }
}
