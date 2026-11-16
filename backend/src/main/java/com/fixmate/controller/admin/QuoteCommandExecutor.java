package com.fixmate.controller.admin;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.config.JwtProperties;
import com.fixmate.exception.BusinessException;
import com.fixmate.security.*;
import com.fixmate.service.admin.StaffQuoteService;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class QuoteCommandExecutor {
    private final IdempotentExecutor executor;private final StaffQuoteService service;private final TokenFamilyStore families;private final JwtProperties properties;
    public QuoteCommandExecutor(IdempotentExecutor executor,StaffQuoteService service,TokenFamilyStore families,JwtProperties properties){this.executor=executor;this.service=service;this.families=families;this.properties=properties;}
    public ResponseEntity<Object> run(StaffPrincipal staff,JwtPrincipal customer,Long order,String operation,String method,String path,String key,Object input,boolean decision,Supplier<?> action){
        Long customerId=customer==null?null:customer.userId();String scope=customer==null?"STAFF:"+staff.staffId()+":"+operation:IdempotencyService.scopeForUser(customerId,operation);
        var ctx=new IdempotencyContext(scope,key,operation,executor.hashCommand(method,path,input),customer==null?null:customer.familyId(),true,Math.min(900,properties.getAccessTtlSeconds()),customer!=null);
        return AdminCommandExecutor.noStore(executor.executeCurrentRead(ctx,()->ResponseEntity.ok(ApiResponse.ok(action.get())),()->{
            if(customer!=null&&!families.isFamilyActive(customer.familyId()))throw BusinessException.unauthorized("登录已失效");
            service.guard(staff,customerId,order,decision);
        }));
    }
}
