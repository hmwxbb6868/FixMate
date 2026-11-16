package com.fixmate.controller.admin;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.config.JwtProperties;
import com.fixmate.exception.BusinessException;
import com.fixmate.security.*;
import com.fixmate.service.admin.StaffWorkOrderService;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** 含履约资料的响应短期加密保存；身份与资源授权在每次重放前重查。 */
@Component
public class WorkOrderCommandExecutor {
    private final IdempotentExecutor executor;
    private final StaffWorkOrderService service;
    private final TokenFamilyStore families;
    private final JwtProperties properties;
    public WorkOrderCommandExecutor(IdempotentExecutor executor,StaffWorkOrderService service,TokenFamilyStore families,JwtProperties properties){this.executor=executor;this.service=service;this.families=families;this.properties=properties;}
    public ResponseEntity<Object> admin(StaffPrincipal actor,Long orderId,String operation,String key,String path,Object input,Supplier<?> action){
        var ctx=new IdempotencyContext("STAFF:"+actor.staffId()+":"+operation,key,operation,executor.hashCommand("POST",path,input),null,true,ttl(),false);
        String resolution=input instanceof com.fixmate.dto.admin.StaffIdentityContracts.WorkOrderCoordinationDTO dto?dto.resolution():null;
        return run(ctx,action,()->service.guardAdmin(actor,orderId,operation,resolution));
    }
    public ResponseEntity<Object> technician(JwtPrincipal actor,Long orderId,String operation,String key,String path,Object input,Supplier<?> action){
        var ctx=new IdempotencyContext(IdempotencyService.scopeForUser(actor.userId(),operation),key,operation,executor.hashCommand("POST",path,input),actor.familyId(),true,ttl(),true);
        return run(ctx,action,()->{
            if(!families.isFamilyActive(actor.familyId()))throw BusinessException.unauthorized("登录已失效");
            if("technicianCompletion".equals(operation))service.guardCompletion(actor.userId(),orderId);
            else if("technicianDeclineReschedule".equals(operation))service.participation(actor.userId(),orderId);
            else service.guardTechnician(actor.userId(),orderId);
        });
    }
    private long ttl(){return Math.min(900,properties.getAccessTtlSeconds());}
    private ResponseEntity<Object> run(IdempotencyContext ctx,Supplier<?> action,Runnable guard){
        return AdminCommandExecutor.noStore(executor.executeCurrentRead(ctx,()->ResponseEntity.ok(ApiResponse.ok(action.get())),guard));
    }
}
