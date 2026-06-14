package com.lab.ai.pusaman.annotation;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = pjp.getSignature().toLongString();
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> RateLimiter.create(rateLimit.qps()));

        if (limiter.tryAcquire()) {
            return pjp.proceed();
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("RateLimit triggered on [{}] but no request context available", key);
            return null;
        }

        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            log.warn("RateLimit triggered on [{}] but response is null", key);
            return null;
        }

        response.sendError(429, "Too Many Requests");
        return null;
    }
}
