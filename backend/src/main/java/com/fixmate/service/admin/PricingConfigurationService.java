package com.fixmate.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.common.EmployeeWechatAccessService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** B3配置依赖；配置写先锁员工目录，服务项目→价格规则按ID锁定。 */
@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class PricingConfigurationService {
    private static final String[] READ={"ADMIN","KNOWLEDGE_MANAGER","TECHNICIAN","DISPATCHER","AUDITOR"};
    private final com.fixmate.service.common.PricingApprovalInvalidation invalidation;
    private final ServiceItemMapper items;
    private final PricingRuleMapper rules;
    private final StaffAccessService access;
    private final StaffIdentityGuard identities;
    private final EmployeeWechatAccessService employees;
    private final DatabaseClockMapper clock;
    private final AuditLogService audit;
    public PricingConfigurationService(ServiceItemMapper items,PricingRuleMapper rules,StaffAccessService access,
            StaffIdentityGuard identities,EmployeeWechatAccessService employees,DatabaseClockMapper clock,AuditLogService audit,com.fixmate.service.common.PricingApprovalInvalidation invalidation) {
        this.invalidation=invalidation;
        this.items=items;this.rules=rules;this.access=access;this.identities=identities;
        this.employees=employees;this.clock=clock;this.audit=audit;
    }
    public void guard(StaffPrincipal actor){access.require(actor,"ADMIN","KNOWLEDGE_MANAGER");}
    private void writer(StaffPrincipal actor){identities.requireRoles(actor,identities.lockDirectory(),"ADMIN","KNOWLEDGE_MANAGER");}
    public ServiceItemStaffVOPageVO listItems(StaffPrincipal actor,Long customer,int page,int size,String appliance) {
        authorize(actor,customer);paging(page,size);appliance(appliance);
        var q=new QueryWrapper<ServiceItemEntity>();
        if(customer!=null)q.eq("status","ENABLED");
        if(appliance!=null)q.eq("appliance_type",appliance);
        var result=items.selectPage(new Page<>(page,size),q.orderByAsc("id"));
        return new ServiceItemStaffVOPageVO(result.getRecords().stream().map(this::view).toList(),page,size,result.getTotal());
    }
    public PricingRuleStaffVOPageVO listRules(StaffPrincipal actor,Long customer,int page,int size,Long serviceItem,String appliance) {
        authorize(actor,customer);paging(page,size);appliance(appliance);
        if(serviceItem!=null&&serviceItem<=0)throw BusinessException.validation("服务项目ID不正确");
        var now=clock.now();var q=new QueryWrapper<PricingRuleEntity>();
        if(serviceItem!=null)q.eq("service_item_id",serviceItem);
        if(customer!=null)q.eq("status","ENABLED").le("effective_from",now)
                .and(t->t.isNull("effective_to").or().gt("effective_to",now))
                .apply("EXISTS (SELECT 1 FROM service_item si WHERE si.id=pricing_rule.service_item_id AND si.status='ENABLED')");
        if(appliance!=null)q.apply("EXISTS (SELECT 1 FROM service_item si WHERE si.id=pricing_rule.service_item_id AND si.appliance_type={0})",appliance);
        var result=rules.selectPage(new Page<>(page,size),q.orderByAsc("id"));
        return new PricingRuleStaffVOPageVO(result.getRecords().stream().map(this::view).toList(),page,size,result.getTotal());
    }
    private void authorize(StaffPrincipal actor,Long customer){if(customer==null)access.require(actor,READ);else employees.require(customer);}
    public ServiceItemStaffVO getItem(StaffPrincipal actor,Long id){access.require(actor,READ);return view(item(id,false));}
    public PricingRuleStaffVO getRule(StaffPrincipal actor,Long id){access.require(actor,READ);return view(rule(id,false));}
    public ServiceItemStaffVO createItem(StaffPrincipal actor,ServiceItemInputDTO input) {
        writer(actor);var row=new ServiceItemEntity();row.setVersion(0);row.setStatus("ENABLED");
        fill(row,input.code(),input.applianceType(),input.name(),input.description());uniqueItem(row);
        row.setCreatedAt(clock.now());row.setUpdatedAt(row.getCreatedAt());items.insert(row);
        record(actor,"SERVICE_ITEM_CREATED","SERVICE_ITEM",row.getId(),null,view(row));return view(row);
    }
    public ServiceItemStaffVO updateItem(StaffPrincipal actor,Long id,ServiceItemUpdateDTO input) {
        writer(actor);invalidation.invalidate(id,null);var row=item(id,true);StaffIdentityGuard.version(row.getVersion(),input.expectedVersion());var before=view(row);
        fill(row,input.code(),input.applianceType(),input.name(),input.description());uniqueItem(row);
        row.setVersion(row.getVersion()+1);row.setUpdatedAt(clock.now());items.updateById(row);
        record(actor,"SERVICE_ITEM_UPDATED","SERVICE_ITEM",id,before,view(row));return view(row);
    }
    public ServiceItemStaffVO statusItem(StaffPrincipal actor,Long id,ConfigStatusDTO input) {
        writer(actor);status(input);invalidation.invalidate(id,null);var row=item(id,true);StaffIdentityGuard.version(row.getVersion(),input.expectedVersion());var before=view(row);
        if(row.getStatus().equals(input.status()))throw BusinessException.stateConflict("服务项目已处于目标状态");
        row.setStatus(input.status());row.setVersion(row.getVersion()+1);row.setUpdatedAt(clock.now());items.updateById(row);
        record(actor,"SERVICE_ITEM_STATUS","SERVICE_ITEM",id,before,Map.of("version",row.getVersion(),"status",row.getStatus(),"reason",input.reason()));return view(row);
    }
    public PricingRuleStaffVO createRule(StaffPrincipal actor,PricingRuleInputDTO input) {
        writer(actor);var row=new PricingRuleEntity();row.setVersion(0);row.setStatus("ENABLED");
        fill(row,input.serviceItemId(),input.feeType(),input.clauseNo(),input.ruleVersion(),input.title(),input.description(),input.unit(),
                input.fixedPrice(),input.minPrice(),input.maxPrice(),input.refundPolicy(),input.effectiveFrom(),input.effectiveTo());
        item(row.getServiceItemId(),true);validateRule(row);row.setCreatedBy(actor.staffId());row.setUpdatedBy(actor.staffId());
        row.setCreatedAt(clock.now());row.setUpdatedAt(row.getCreatedAt());rules.insert(row);
        record(actor,"PRICING_RULE_CREATED","PRICING_RULE",row.getId(),null,view(row));return view(row);
    }
    public PricingRuleStaffVO updateRule(StaffPrincipal actor,Long id,PricingRuleUpdateDTO input) {
        writer(actor);invalidation.invalidate(null,id);
        // 同时锁新旧服务项目后再锁规则；所有后台配置写由目录锁串行。
        var candidate=rule(id,false);var ids=new TreeSet<Long>();ids.add(candidate.getServiceItemId());ids.add(input.serviceItemId());for(var value:ids)item(value,true);
        var row=rule(id,true);StaffIdentityGuard.version(row.getVersion(),input.expectedVersion());var before=view(row);
        fill(row,input.serviceItemId(),input.feeType(),input.clauseNo(),input.ruleVersion(),input.title(),input.description(),input.unit(),
                input.fixedPrice(),input.minPrice(),input.maxPrice(),input.refundPolicy(),input.effectiveFrom(),input.effectiveTo());
        validateRule(row);row.setVersion(row.getVersion()+1);row.setUpdatedBy(actor.staffId());row.setUpdatedAt(clock.now());rules.updateById(row);
        record(actor,"PRICING_RULE_UPDATED","PRICING_RULE",id,before,view(row));return view(row);
    }
    public PricingRuleStaffVO statusRule(StaffPrincipal actor,Long id,ConfigStatusDTO input) {
        writer(actor);status(input);invalidation.invalidate(null,id);item(rule(id,false).getServiceItemId(),true);var row=rule(id,true);
        StaffIdentityGuard.version(row.getVersion(),input.expectedVersion());var before=view(row);
        if(row.getStatus().equals(input.status()))throw BusinessException.stateConflict("价格规则已处于目标状态");
        row.setStatus(input.status());validateRule(row);row.setVersion(row.getVersion()+1);row.setUpdatedBy(actor.staffId());row.setUpdatedAt(clock.now());rules.updateById(row);
        record(actor,"PRICING_RULE_STATUS","PRICING_RULE",id,before,Map.of("version",row.getVersion(),"status",row.getStatus(),"reason",input.reason()));return view(row);
    }
    private void uniqueItem(ServiceItemEntity row){
        var q=new QueryWrapper<ServiceItemEntity>().eq("code",row.getCode());if(row.getId()!=null)q.ne("id",row.getId());
        if(items.selectCount(q)>0)throw BusinessException.stateConflict("服务项目编码已存在");
    }
    private void validateRule(PricingRuleEntity row) {
        validatePrices(row.getFixedPrice(),row.getMinPrice(),row.getMaxPrice());
        if(row.getEffectiveTo()!=null&&!row.getEffectiveTo().isAfter(row.getEffectiveFrom()))throw BusinessException.validation("规则结束时间须晚于开始时间");
        var unique=new QueryWrapper<PricingRuleEntity>().eq("clause_no",row.getClauseNo()).eq("rule_version",row.getRuleVersion());
        if(row.getId()!=null)unique.ne("id",row.getId());
        if(rules.selectCount(unique)>0)throw BusinessException.stateConflict("条款及版本已存在");
        if(!"ENABLED".equals(row.getStatus()))return;
        if("VISIT".equals(row.getFeeType())&&(row.getFixedPrice()==null
                ||row.getFixedPrice().compareTo(com.fixmate.service.common.CustomerCancellationPolicy.STANDARD_VISIT_FEE)!=0))
            throw BusinessException.validation("上门费统一为固定 50 元");
        var q=new QueryWrapper<PricingRuleEntity>().eq("service_item_id",row.getServiceItemId()).eq("fee_type",row.getFeeType()).eq("status","ENABLED")
                .and(t->t.isNull("effective_to").or().gt("effective_to",row.getEffectiveFrom()));
        if(row.getEffectiveTo()!=null)q.lt("effective_from",row.getEffectiveTo());if(row.getId()!=null)q.ne("id",row.getId());
        if(rules.selectCount(q)>0)throw BusinessException.stateConflict("同服务项目和费用类型的有效规则时间重叠");
    }
    public static void validatePrices(BigDecimal fixed,BigDecimal min,BigDecimal max) {
        if(fixed!=null?(min!=null||max!=null):(min==null||max==null||min.compareTo(max)>0))throw BusinessException.validation("固定价与完整区间须二选一，且最低价不得高于最高价");
        for(var price:Arrays.asList(fixed,min,max))if(price!=null&&(price.signum()<0||price.scale()>2||price.compareTo(new BigDecimal("9999999999.99"))>0))throw BusinessException.validation("价格超出金额范围");
    }
    private void fill(ServiceItemEntity row,String code,String appliance,String name,String description) {
        appliance(appliance);row.setCode(text(code,1,64,"编码"));row.setApplianceType(appliance);row.setName(text(name,1,200,"名称"));row.setDescription(text(description,0,1000,"说明"));
    }
    private void fill(PricingRuleEntity row,Long service,String fee,String clause,String version,String title,String description,String unit,
            String fixed,String min,String max,String refund,OffsetDateTime from,OffsetDateTime to) {
        if(!Set.of("VISIT","INSPECTION","LABOR","PART","MATERIAL","OTHER").contains(fee))throw BusinessException.validation("费用类型不正确");
        row.setServiceItemId(service);row.setFeeType(fee);row.setClauseNo(text(clause,1,64,"条款号"));row.setRuleVersion(text(version,1,64,"规则版本"));
        row.setTitle(text(title,1,200,"条款标题"));row.setDescription(text(description,0,2000,"条款说明"));row.setUnit(text(unit,1,32,"单位"));
        row.setFixedPrice(money(fixed));row.setMinPrice(money(min));row.setMaxPrice(money(max));
        row.setRefundPolicy(text(refund,Set.of("VISIT","INSPECTION").contains(fee)?1:0,1000,"退费条款"));
        if(from==null)throw BusinessException.validation("生效时间不能为空");
        row.setEffectiveFrom(from.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));row.setEffectiveTo(to==null?null:to.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }
    private BigDecimal money(String value){if(value==null)return null;if(!value.matches("^[0-9]{1,10}\\.[0-9]{2}$"))throw BusinessException.validation("金额格式不正确");return new BigDecimal(value);}
    private String text(String value,int min,int max,String label){return StaffIdentityGuard.text(value,min,max,label);}
    private void status(ConfigStatusDTO input){if(!Set.of("ENABLED","DISABLED").contains(input.status()))throw BusinessException.validation("状态不正确");text(input.reason(),2,1000,"原因");}
    private void appliance(String value){if(value!=null&&!Set.of("AIR_CONDITIONER","REFRIGERATOR","WASHING_MACHINE","AIR_SOURCE_HEAT_PUMP").contains(value))throw BusinessException.validation("设备类型不正确");}
    private void paging(int page,int size){if(page<1||size<1||size>100)throw BusinessException.validation("分页参数不正确");}
    private ServiceItemEntity item(Long id,boolean lock){var row=items.selectOne(new QueryWrapper<ServiceItemEntity>().eq("id",id).last(lock?"FOR UPDATE":""));if(row==null)throw BusinessException.notFound("服务项目不存在");return row;}
    private PricingRuleEntity rule(Long id,boolean lock){var row=rules.selectOne(new QueryWrapper<PricingRuleEntity>().eq("id",id).last(lock?"FOR UPDATE":""));if(row==null)throw BusinessException.notFound("价格规则不存在");return row;}
    private ServiceItemStaffVO view(ServiceItemEntity row){return new ServiceItemStaffVO(row.getId(),row.getCode(),row.getApplianceType(),row.getName(),Objects.toString(row.getDescription(),""),row.getStatus(),row.getVersion());}
    private PricingRuleStaffVO view(PricingRuleEntity r){return new PricingRuleStaffVO(r.getId(),r.getServiceItemId(),r.getFeeType(),r.getClauseNo(),r.getRuleVersion(),r.getTitle(),Objects.toString(r.getDescription(),""),r.getUnit(),amount(r.getFixedPrice()),amount(r.getMinPrice()),amount(r.getMaxPrice()),Objects.toString(r.getRefundPolicy(),""),offset(r.getEffectiveFrom()),offset(r.getEffectiveTo()),r.getStatus(),r.getVersion());}
    private String amount(BigDecimal v){return v==null?null:v.setScale(2).toPlainString();}
    private OffsetDateTime offset(LocalDateTime v){return v==null?null:v.atOffset(ZoneOffset.UTC);}
    private void record(StaffPrincipal actor,String action,String type,Long id,Object before,Object after){audit.record("STAFF",actor.staffId(),action,type,id,before,after);}
}
