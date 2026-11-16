package com.fixmate.security;

import com.fixmate.config.JwtProperties;
import com.fixmate.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class StaffAttemptLimiter {
    private final StringRedisTemplate redis;private final byte[] secret;
    private final StaffSecurityAudit audit;
    private static final DefaultRedisScript<Long> FAIL=new DefaultRedisScript<>("local n=redis.call('INCR',KEYS[1]);if n==1 or n==tonumber(ARGV[2]) then redis.call('EXPIRE',KEYS[1],ARGV[1]) end;return n",Long.class);
    private static final DefaultRedisScript<Long> HIT=new DefaultRedisScript<>("local n=redis.call('INCR',KEYS[1]);if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end;return n",Long.class);
    public StaffAttemptLimiter(StringRedisTemplate redis,JwtProperties p,StaffSecurityAudit audit){this.redis=redis;secret=p.getSecret().getBytes(StandardCharsets.UTF_8);this.audit=audit;}
    public String digest(String value) {
        try {Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
    }
    private String key(String purpose,String subject){return "fixmate:staff:attempt:"+purpose+":"+digest(subject);}
    public void check(String purpose,String subject,int max){String n=redis.opsForValue().get(key(purpose,subject));if(n!=null&&Long.parseLong(n)>=max)throw limited(purpose);}
    public void failed(String purpose,String subject,int seconds,int threshold){
        if(redis.execute(FAIL,List.of(key(purpose,subject)),Integer.toString(seconds),Integer.toString(threshold))==null)throw new IllegalStateException("staff limiter unavailable");
    }
    public void hit(String purpose,String subject,int max,int seconds) {
        Long count=redis.execute(HIT,List.of(key(purpose,subject)),Integer.toString(seconds));
        if(count==null)throw new IllegalStateException("staff limiter unavailable");if(count>max)throw limited(purpose);
    }
    private BusinessException limited(String purpose) {
        audit.rateLimited(purpose);
        return new BusinessException("RATE_LIMITED",org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"操作过于频繁，请稍后重试");
    }
}
