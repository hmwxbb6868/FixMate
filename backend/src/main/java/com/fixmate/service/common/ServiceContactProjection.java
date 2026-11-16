package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.dto.admin.StaffIdentityContracts.ServiceContactVO;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.CustomerAddressMapper;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** 私有服务联系数据只能经已授权的业务用例调用，不提供用户HTTP直读。 */
@Service
public class ServiceContactProjection {
    private final ObjectMapper json;
    private final CustomerAddressMapper addresses;
    public ServiceContactProjection(ObjectMapper json,CustomerAddressMapper addresses){this.json=json;this.addresses=addresses;}

    public ServiceContactVO view(RepairRequestEntity request,boolean disclose) {
        return view(request,disclose,request.getAddressSnapshot());
    }
    private ServiceContactVO view(RepairRequestEntity request,boolean disclose,String addressSnapshot) {
        if(!disclose){String source=text(parse(request.getServiceContactSnapshot()),"contactSource");if(!java.util.Set.of("SUBMISSION_SNAPSHOT","MANUAL_REVIEW_SNAPSHOT").contains(source==null?"":source))source="UNAVAILABLE";return new ServiceContactVO(null,null,null,source,true,null);}
        String name=null,phone=null,source="UNAVAILABLE";Integer version=null;
        if(request.getServiceContactSnapshot()!=null) {
            var data=parse(request.getServiceContactSnapshot());name=text(data,"contactName");phone=text(data,"contactPhone");
            source=text(data,"contactSource");
            if(!java.util.Set.of("SUBMISSION_SNAPSHOT","MANUAL_REVIEW_SNAPSHOT").contains(source==null?"":source)) {
                name=null;phone=null;source="UNAVAILABLE";
            }
        } else if(!"DRAFT".equals(request.getStatus())) {
            var address=request.getAddressId()==null?null:addresses.selectById(request.getAddressId());
            if(owned(request,address)) {name=address.getContactName();phone=address.getContactPhone();source="CURRENT_ADDRESS";version=address.getVersion();}
        }
        String addressText=addressText(addressSnapshot);
        return new ServiceContactVO(name,phone,addressText,source,false,version);
    }
    /** 报修根行锁之后调用，旧记录补齐必须锁有效本人地址并比对管理员所见版本。 */
    public String confirmLegacy(RepairRequestEntity request,Integer expectedAddressVersion) {
        if(expectedAddressVersion==null)throw BusinessException.validation("旧报修需确认地址版本");
        var address=request.getAddressId()==null?null:addresses.selectOne(new LambdaQueryWrapper<CustomerAddressEntity>()
                .eq(CustomerAddressEntity::getId,request.getAddressId()).last("FOR UPDATE"));
        if(!owned(request,address))throw BusinessException.stateConflict("服务联系地址已不可用，请核实");
        if(!expectedAddressVersion.equals(address.getVersion()))throw BusinessException.versionConflict("联系地址已变化，请刷新重新核对");
        if(address.getContactName()==null||address.getContactName().isBlank()||!usablePhone(address.getContactPhone()))
            throw BusinessException.validation("缺少可核对的完整服务联系信息");
        var data=new LinkedHashMap<String,Object>();data.put("contactName",address.getContactName());data.put("contactPhone",address.getContactPhone());
        data.put("contactSource","MANUAL_REVIEW_SNAPSHOT");
        try{return json.writeValueAsString(data);}catch(Exception ex){throw new IllegalStateException("contact serialization failed",ex);}
    }
    public boolean complete(RepairRequestEntity request) {
        return complete(view(request,true));
    }
    public boolean confirmedForOrder(RepairRequestEntity request,WorkOrderEntity order) {
        if(request==null||request.getServiceContactSnapshot()==null
                ||!Objects.equals(request.getId(),order.getRepairRequestId())
                ||!Objects.equals(request.getWorkOrderId(),order.getId())
                ||!Objects.equals(request.getUserId(),order.getCustomerId()))return false;
        // 旧紧急单的原始上门地址可能只保存在关联工单中。
        String address=request.getAddressSnapshot()==null?order.getAddressSnapshot():request.getAddressSnapshot();
        return complete(view(request,true,address));
    }
    private boolean complete(ServiceContactVO contact) {
        return contact.contactName()!=null&&!contact.contactName().isBlank()&&usablePhone(contact.contactPhone())
                &&contact.addressText()!=null&&!contact.addressText().isBlank();
    }
    public String redactNote(String note,RepairRequestEntity request) {
        String phone=view(request,true).contactPhone();
        if(phone!=null&&!phone.isBlank())note=note.replace(phone,mask(phone));
        return note.replaceAll("(?<![0-9])([0-9]{3})[0-9]{4}([0-9]{4})(?![0-9])","$1****$2");
    }
    public String redactPersonalText(String value,RepairRequestEntity request){
        if(value==null)return null;var contact=view(request,true);
        for(var text:new String[]{contact.contactName(),contact.addressText(),text(parse(request.getAddressSnapshot()),"detail")})if(text!=null&&!text.isBlank())value=value.replace(text,"＊");
        return redactNote(value,request);
    }
    private boolean owned(RepairRequestEntity request,CustomerAddressEntity a){return a!=null&&request.getUserId().equals(a.getUserId())&&"ENABLED".equals(a.getStatus());}
    private boolean usablePhone(String value){return value!=null&&value.matches("[0-9+() -]{6,20}")&&value.chars().filter(Character::isDigit).count()>=6;}
    private JsonNode parse(String value){try{return value==null?json.createObjectNode():json.readTree(value);}catch(Exception ex){return json.createObjectNode();}}
    private String text(JsonNode node,String field){JsonNode v=node==null?null:node.get(field);return v!=null&&v.isTextual()?v.asText():null;}
    private String addressText(String snapshot) {
        var data=parse(snapshot);var parts=new java.util.ArrayList<String>();
        for(String key:java.util.List.of("province","city","district","detail")){String value=text(data,key);if(value!=null&&!value.isBlank())parts.add(value);}
        return parts.isEmpty()?null:String.join("",parts);
    }
    private String mask(String phone){return phone==null?null:phone.length()<7?"***":phone.substring(0,3)+"****"+phone.substring(phone.length()-4);}
}
