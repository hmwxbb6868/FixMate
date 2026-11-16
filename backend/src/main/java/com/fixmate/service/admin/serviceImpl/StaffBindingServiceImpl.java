package com.fixmate.service.admin.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.security.wechat.WechatClient;
import com.fixmate.service.admin.StaffBindingService;
import com.fixmate.service.admin.StaffDirectoryService;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffBindingServiceImpl implements StaffBindingService {
    private final StaffInvitationMapper invites;
    private final StaffWechatBindingMapper bindings;
    private final StaffUserMapper staff;
    private final CustomerUserMapper customers;
    private final StaffIdentityGuard guard;
    private final StaffAccessService access;
    private final StaffAttemptLimiter attempts;
    private final AuditLogService audit;
    private final DatabaseClockMapper clock;
    private final StaffDirectoryService directoryService;
    private final WechatClient wechat;
    private final SecureRandom random=new SecureRandom();
    public StaffBindingServiceImpl(StaffInvitationMapper invites,StaffWechatBindingMapper bindings,StaffUserMapper staff,
            CustomerUserMapper customers,StaffIdentityGuard guard,StaffAccessService access,StaffAttemptLimiter attempts,
            AuditLogService audit,DatabaseClockMapper clock,StaffDirectoryService directoryService,WechatClient wechat) {
        this.invites=invites;this.bindings=bindings;this.staff=staff;this.customers=customers;this.guard=guard;
        this.access=access;this.attempts=attempts;this.audit=audit;this.clock=clock;
        this.directoryService=directoryService;this.wechat=wechat;
    }
    @Override @Transactional public StaffInvitationIssuedVO issue(StaffPrincipal actor,Long staffId,StaffInvitationCreateDTO input) {
        var directory=guard.lockDirectory();guard.requireAdmin(actor,directory);var employee=guard.target(directory,staffId);
        StaffIdentityGuard.version(employee.getVersion(),input.expectedStaffVersion());requireTechnician(employee);
        if(bindings.selectCount(new LambdaQueryWrapper<StaffWechatBindingEntity>()
                .eq(StaffWechatBindingEntity::getStaffId,staffId)
                .in(StaffWechatBindingEntity::getStatus,List.of("PENDING","ACTIVE")))>0) {
            throw BusinessException.stateConflict("该员工已有待审核或有效维修身份，请先到维修身份审核查看");
        }
        String code=newInvitationCode();
        var invite=saveInvitation(actor,staffId,code,now().plusHours(24));
        return new StaffInvitationIssuedVO(invitationView(invite),code,"pages/staff/bind/index");
    }
    @Override @Transactional public NewTechnicianInvitationIssuedVO issueNewTechnician(StaffPrincipal actor) {
        access.require(actor,"ADMIN");
        String code=newInvitationCode();
        LocalDateTime expiresAt=now().plusHours(24).withNano(0);
        String url=wechat.generateMiniProgramLink("pages/workspace/binding/index","invitationCode="+code,
                expiresAt.toInstant(ZoneOffset.UTC));
        guard.requireAdmin(actor,guard.lockDirectory());
        return new NewTechnicianInvitationIssuedVO(invitationView(saveInvitation(actor,null,code,expiresAt)),url,
                url==null?code:null);
    }
    private String newInvitationCode() {
        byte[] bytes=new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private StaffInvitationEntity saveInvitation(StaffPrincipal actor,Long staffId,String code,LocalDateTime expiresAt) {
        var invite=new StaffInvitationEntity();invite.setStaffId(staffId);invite.setCodeHash(inviteHash(code));invite.setStatus("ISSUED");
        invite.setExpiresAt(expiresAt);invite.setCreatedBy(actor.staffId());invite.setVersion(0);invite.setCreatedAt(now());invite.setUpdatedAt(now());
        invites.insert(invite);
        audit.record("STAFF",actor.staffId(),"STAFF_INVITATION_ISSUED","STAFF_INVITATION",invite.getId(),null,
                staffId==null?Map.of("target","NEW_TECHNICIAN"):Map.of("staffId",staffId));
        return invite;
    }
    @Override public StaffInvitationVOPageVO invitations(StaffPrincipal actor,int page,int size,Long staffId,String status) {
        access.require(actor,"ADMIN");StaffDirectoryServiceImpl.page(page,size);
        var query=new LambdaQueryWrapper<StaffInvitationEntity>();if(staffId!=null)query.eq(StaffInvitationEntity::getStaffId,staffId);
        if(status!=null) {
            if(!Set.of("ISSUED","CONSUMED","REVOKED","EXPIRED").contains(status))throw BusinessException.validation("邀请状态不正确");
            if("EXPIRED".equals(status))query.and(q->q.eq(StaffInvitationEntity::getStatus,"EXPIRED").or()
                    .eq(StaffInvitationEntity::getStatus,"ISSUED").le(StaffInvitationEntity::getExpiresAt,now()));
            else {query.eq(StaffInvitationEntity::getStatus,status);if("ISSUED".equals(status))query.gt(StaffInvitationEntity::getExpiresAt,now());}
        }
        var result=invites.selectPage(new Page<>(page,size),query.orderByDesc(StaffInvitationEntity::getId));
        return new StaffInvitationVOPageVO(result.getRecords().stream().map(this::invitationView).toList(),page,size,result.getTotal());
    }
    @Override public StaffInvitationVO invitation(StaffPrincipal actor,Long id) {
        access.require(actor,"ADMIN");var invite=invites.selectById(id);if(invite==null)throw BusinessException.notFound("邀请不存在");return invitationView(invite);
    }
    @Override @Transactional public StaffInvitationVO revokeInvitation(StaffPrincipal actor,Long id,ReasonCommandDTO input) {
        guard.requireAdmin(actor,guard.lockDirectory());
        var invite=invites.selectOne(new LambdaQueryWrapper<StaffInvitationEntity>().eq(StaffInvitationEntity::getId,id).last("FOR UPDATE"));
        if(invite==null)throw BusinessException.notFound("邀请不存在");StaffIdentityGuard.version(invite.getVersion(),input.expectedVersion());
        if(!"ISSUED".equals(effectiveStatus(invite)))throw BusinessException.stateConflict("邀请已失效或已使用");
        String reason=StaffIdentityGuard.text(input.reason(),2,1000,"撤销原因");
        invite.setStatus("REVOKED");invite.setRevokedBy(actor.staffId());invite.setRevokedAt(now());invite.setRevocationReason(reason);
        check(invites.update(null,new UpdateWrapper<StaffInvitationEntity>().eq("id",id).eq("version",input.expectedVersion())
                .set("status","REVOKED").set("revoked_by",actor.staffId()).set("revoked_at",invite.getRevokedAt()).set("revocation_reason",reason)
                .set("version",input.expectedVersion()+1).set("updated_at",now())));
        invite.setVersion(input.expectedVersion()+1);
        audit.record("STAFF",actor.staffId(),"STAFF_INVITATION_REVOKED","STAFF_INVITATION",id,null,Map.of("reason",reason));
        return invitationView(invite);
    }
    @Override @Transactional public StaffBindingVO apply(Long customerId,StaffBindingApplyDTO input,String ip) {
        attempts.check("binding-user",customerId.toString(),5);attempts.check("binding-ip",ip,5);
        var directory=guard.lockDirectory();
        var customer=customers.selectOne(new LambdaQueryWrapper<CustomerUserEntity>().eq(CustomerUserEntity::getId,customerId).last("FOR UPDATE"));
        requireCustomerEntity(customer);
        var invite=invites.selectOne(new LambdaQueryWrapper<StaffInvitationEntity>().eq(StaffInvitationEntity::getCodeHash,inviteHash(input.invitationCode())).last("FOR UPDATE"));
        if(invite==null||!"ISSUED".equals(effectiveStatus(invite)))throw invalidInvitation(customerId,ip);
        Long staffId=invite.getStaffId();
        if(staffId!=null) {
            var employee=directory.stream().filter(s->s.getId().equals(staffId)).findFirst().orElse(null);
            if(employee==null||!"ENABLED".equals(employee.getStatus())||!"TECHNICIAN".equals(employee.getRole()))throw invalidInvitation(customerId,ip);
        }
        String name=StaffIdentityGuard.text(input.applicantName(),1,64,"申请人姓名");
        String phone=StaffIdentityGuard.text(input.applicantContactPhone(),6,20,"联系电话");
        if(!phone.matches("^[0-9+() -]{6,20}$"))throw BusinessException.validation("联系电话格式不正确");
        var b=new StaffWechatBindingEntity();b.setCustomerId(customerId);b.setStaffId(staffId);b.setInvitationId(invite.getId());
        b.setStatus("PENDING");b.setApplicantName(name);b.setApplicantContactPhone(phone);b.setContactPhoneVerified(false);
        b.setVersion(0);b.setAppliedAt(now());b.setCreatedAt(now());b.setUpdatedAt(now());
        try {bindings.insert(b);}catch(DuplicateKeyException ex){throw BusinessException.stateConflict("该微信或员工已有待审核或有效绑定，请联系管理员");}
        check(invites.update(null,new UpdateWrapper<StaffInvitationEntity>().eq("id",invite.getId()).eq("version",invite.getVersion())
                .eq("status","ISSUED").gt("expires_at",now()).set("status","CONSUMED").set("consumed_at",now()).set("consumed_binding_id",b.getId())
                .set("version",invite.getVersion()+1).set("updated_at",now())));
        // 不把邀请码、手机号、密码或令牌写入审计；申请人资料仅保存在权限受控的业务表中。
        audit.recordCustomer(customerId,"STAFF_BINDING_APPLIED","STAFF_BINDING",b.getId(),null,bindingAudit(b));
        return bindingView(b);
    }
    private BusinessException invalidInvitation(Long customerId,String ip) {
        attempts.failed("binding-user",customerId.toString(),600,5);attempts.failed("binding-ip",ip,600,5);
        return BusinessException.validation("邀请码无效或不可用，请联系管理员");
    }
    @Override public StaffBindingVO current(Long customerId) {
        requireCustomer(customerId);
        var b=bindings.selectOne(new LambdaQueryWrapper<StaffWechatBindingEntity>().eq(StaffWechatBindingEntity::getCustomerId,customerId)
                .orderByDesc(StaffWechatBindingEntity::getId).last("LIMIT 1"));
        if(b==null)throw BusinessException.notFound("暂无员工绑定申请");return bindingView(b);
    }
    @Override public WorkspaceAccessVO workspace(Long customerId) {
        requireCustomer(customerId);
        var b=bindings.selectOne(new LambdaQueryWrapper<StaffWechatBindingEntity>().eq(StaffWechatBindingEntity::getCustomerId,customerId)
                .eq(StaffWechatBindingEntity::getStatus,"ACTIVE"));
        var employee=b==null?null:staff.selectById(b.getStaffId());
        if(employee==null||!"ENABLED".equals(employee.getStatus())||!"TECHNICIAN".equals(employee.getRole()))
            return new WorkspaceAccessVO(List.of("CUSTOMER"),null,null,null,null,null,null);
        return new WorkspaceAccessVO(List.of("CUSTOMER","TECHNICIAN"),employee.getId().toString(),employee.getName(),employee.getPhone(),"TECHNICIAN",b.getId().toString(),b.getVersion());
    }
    @Override public StaffBindingVOPageVO bindings(StaffPrincipal actor,int page,int size,String status) {
        access.require(actor,"ADMIN");StaffDirectoryServiceImpl.page(page,size);
        if(!Set.of("PENDING","ACTIVE","REJECTED","REVOKED").contains(status))throw BusinessException.validation("绑定状态不正确");
        var result=bindings.selectPage(new Page<>(page,size),new LambdaQueryWrapper<StaffWechatBindingEntity>()
                .eq(StaffWechatBindingEntity::getStatus,status).orderByDesc(StaffWechatBindingEntity::getId));
        return new StaffBindingVOPageVO(result.getRecords().stream().map(this::bindingView).toList(),page,size,result.getTotal());
    }
    @Override public StaffBindingVO binding(StaffPrincipal actor,Long id) {
        access.require(actor,"ADMIN");var b=bindings.selectById(id);if(b==null)throw BusinessException.notFound("绑定申请不存在");return bindingView(b);
    }
    @Override @Transactional public StaffBindingVO decide(StaffPrincipal actor,Long id,StaffBindingDecisionDTO input,String decision) {
        var directory=guard.lockDirectory();guard.requireAdmin(actor,directory);
        var b=bindings.selectOne(new LambdaQueryWrapper<StaffWechatBindingEntity>().eq(StaffWechatBindingEntity::getId,id).last("FOR UPDATE"));
        if(b==null)throw BusinessException.notFound("绑定申请不存在");StaffIdentityGuard.version(b.getVersion(),input.expectedVersion());
        if(!Set.of("ACTIVE","REJECTED","REVOKED").contains(decision))throw BusinessException.validation("审核动作不正确");
        boolean pending="PENDING".equals(b.getStatus());
        if(!pending&&!("REVOKED".equals(decision)&&"ACTIVE".equals(b.getStatus())))throw BusinessException.stateConflict("申请已被处理，请刷新");
        String reason=StaffIdentityGuard.text(input.reason(),2,1000,"审核依据");String previous=b.getStatus();
        var update=new UpdateWrapper<StaffWechatBindingEntity>().eq("id",id).eq("version",input.expectedVersion()).eq("status",previous)
                .set("status",decision).set("decision_reason",reason).set("version",input.expectedVersion()+1).set("updated_at",now());
        if("ACTIVE".equals(decision)) {
            requireCustomerEntity(customers.selectOne(new LambdaQueryWrapper<CustomerUserEntity>().eq(CustomerUserEntity::getId,b.getCustomerId()).last("FOR UPDATE")));
            if(b.getStaffId()==null) {
                String username="tech-"+UUID.randomUUID();
                var employee=directoryService.create(actor,new StaffCreateDTO(username,b.getApplicantName(),null,"TECHNICIAN",null));
                b.setStaffId(employee.id());
                update.set("staff_id",employee.id());
                check(invites.update(null,new UpdateWrapper<StaffInvitationEntity>().eq("id",b.getInvitationId())
                        .eq("consumed_binding_id",b.getId()).isNull("staff_id")
                        .set("staff_id",employee.id()).setSql("version = version + 1").set("updated_at",now())));
            } else {
                requireTechnician(guard.target(directory,b.getStaffId()));
            }
            b.setConfirmedBy(actor.staffId());b.setConfirmedAt(now());update.set("confirmed_by",actor.staffId()).set("confirmed_at",b.getConfirmedAt());
        } else if("REJECTED".equals(decision)) {
            b.setRejectedBy(actor.staffId());b.setRejectedAt(now());update.set("rejected_by",actor.staffId()).set("rejected_at",b.getRejectedAt());
        } else {
            b.setRevokedBy(actor.staffId());b.setRevokedAt(now());update.set("revoked_by",actor.staffId()).set("revoked_at",b.getRevokedAt());
        }
        check(bindings.update(null,update));b.setStatus(decision);b.setDecisionReason(reason);b.setVersion(input.expectedVersion()+1);
        var after=bindingAudit(b);
        after.put("reason",reason);
        audit.record("STAFF",actor.staffId(),"STAFF_BINDING_"+decision,"STAFF_BINDING",id,Map.of("status",previous),after);
        return bindingView(b);
    }
    @Override public void requireCustomer(Long id){requireCustomerEntity(customers.selectById(id));}
    private void requireCustomerEntity(CustomerUserEntity customer){if(customer==null||!"ENABLED".equals(customer.getStatus()))throw BusinessException.forbidden("账号不可用");}
    private void requireTechnician(StaffUserEntity employee){if(!"ENABLED".equals(employee.getStatus())||!"TECHNICIAN".equals(employee.getRole()))throw BusinessException.stateConflict("员工不是有效维修人员");}
    private StaffInvitationVO invitationView(StaffInvitationEntity i) {
        return new StaffInvitationVO(i.getId(),i.getStaffId(),effectiveStatus(i),offset(i.getExpiresAt()),i.getVersion(),access.actor(i.getCreatedBy()),
                offset(i.getCreatedAt()),offset(i.getConsumedAt()),i.getConsumedBindingId()==null?null:i.getConsumedBindingId().toString(),
                access.actor(i.getRevokedBy()),offset(i.getRevokedAt()),i.getRevocationReason());
    }
    private StaffBindingVO bindingView(StaffWechatBindingEntity b) {
        var employee=b.getStaffId()==null?null:staff.selectById(b.getStaffId());
        String staffName=employee==null?"新人申请":employee.getName();
        return new StaffBindingVO(b.getId(),b.getStaffId(),b.getCustomerId(),staffName,b.getStatus(),offset(b.getAppliedAt()),offset(b.getConfirmedAt()),
                b.getVersion(),b.getInvitationId(),b.getApplicantName(),b.getApplicantContactPhone(),Boolean.TRUE.equals(b.getContactPhoneVerified()),b.getDecisionReason(),
                access.actor(b.getConfirmedBy()),access.actor(b.getRejectedBy()),offset(b.getRejectedAt()),access.actor(b.getRevokedBy()),offset(b.getRevokedAt()));
    }
    private Map<String,Object> bindingAudit(StaffWechatBindingEntity b) {
        Map<String,Object> summary=new LinkedHashMap<>(Map.of("status",b.getStatus(),"customerId",b.getCustomerId(),"invitationId",b.getInvitationId()));
        if(b.getStaffId()!=null)summary.put("staffId",b.getStaffId());
        return summary;
    }
    private String effectiveStatus(StaffInvitationEntity i){return "ISSUED".equals(i.getStatus())&&!i.getExpiresAt().isAfter(now())?"EXPIRED":i.getStatus();}
    private String inviteHash(String code){return attempts.digest("STAFF_INVITATION:"+code);}
    private void check(int rows){if(rows!=1)throw BusinessException.versionConflict("资料已更新，请刷新后重试");}
    private LocalDateTime now(){return clock.now();}
    private static OffsetDateTime offset(LocalDateTime value){return value==null?null:value.atOffset(ZoneOffset.UTC);}
}
