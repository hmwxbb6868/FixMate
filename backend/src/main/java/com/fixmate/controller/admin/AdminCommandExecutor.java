package com.fixmate.controller.admin;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.security.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

/** 仅本轮无完整联系方式的安全/分派响应使用；新敏感响应仍须单独配置短期加密。 */
@Component
public class AdminCommandExecutor {
    private final IdempotentExecutor executor;
    private final StaffAccessService access;
    public AdminCommandExecutor(IdempotentExecutor executor,StaffAccessService access){this.executor=executor;this.access=access;}
    public ResponseEntity<Object> run(StaffPrincipal actor,String operation,String key,String path,Object input,Supplier<?> action) {
        var context=IdempotencyContext.forStaff(actor.staffId(),operation,key,executor.hashCommand("POST",path,input),86400);
        return noStore(executor.executeCurrentRead(context,()->ResponseEntity.ok(ApiResponse.ok(action.get())),()->access.require(actor,"ADMIN","DISPATCHER")));
    }
    public static ResponseEntity<Object> ok(Object data){return noStore(ResponseEntity.ok(ApiResponse.ok(data)));}
    public static ResponseEntity<Object> noStore(ResponseEntity<?> response){return ResponseEntity.status(response.getStatusCode()).cacheControl(CacheControl.noStore()).body(response.getBody());}
}
