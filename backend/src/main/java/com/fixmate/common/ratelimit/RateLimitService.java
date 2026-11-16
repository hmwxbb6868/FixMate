package com.fixmate.common.ratelimit;

import com.fixmate.common.api.ErrorCode;
import com.fixmate.exception.BusinessException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 简单固定窗口限流：登录与上传等高风险入口使用。
 * 计数落 Redis，多实例共享。
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String bucket, String identifier, int limitPerWindow, Duration window) {
        String key = "fixmate:rate:" + bucket + ":" + identifier;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }
        if (count != null && count > limitPerWindow) {
            throw new BusinessException(ErrorCode.Codes.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS,
                    "请求过于频繁，请稍后再试");
        }
    }
}
