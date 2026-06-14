package com.lab.ai.pusaman.aspect;

import com.lab.ai.pusaman.annotation.RateLimit;
import com.lab.ai.pusaman.annotation.RateLimitAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitAspectTest {

    RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private ProceedingJoinPoint mockJoinPoint(String methodName) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toLongString()).thenReturn(methodName);
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.proceed()).thenReturn("ok");
        return pjp;
    }

    private RateLimit mockAnnotation(double qps) {
        RateLimit annotation = mock(RateLimit.class);
        when(annotation.qps()).thenReturn(qps);
        return annotation;
    }

    private void setRequestContext(MockHttpServletResponse response) {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest(), response));
    }

    @Test
    void proceeds_when_rate_not_exceeded() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint("com.example.TestClass.method()");
        RateLimit annotation = mockAnnotation(1000.0);
        MockHttpServletResponse response = new MockHttpServletResponse();
        setRequestContext(response);

        Object result = aspect.around(pjp, annotation);

        assertEquals("ok", result);
        assertEquals(200, response.getStatus());
        verify(pjp, times(1)).proceed();
    }

    @Test
    void returns_429_when_rate_exceeded() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint("com.example.TestClass.slowMethod()");
        RateLimit annotation = mockAnnotation(0.0001);

        // 先调用一次消耗令牌
        MockHttpServletResponse first = new MockHttpServletResponse();
        setRequestContext(first);
        aspect.around(pjp, annotation);

        // 立即再次调用，应触发限流
        MockHttpServletResponse second = new MockHttpServletResponse();
        setRequestContext(second);
        Object result = aspect.around(pjp, annotation);

        assertNull(result);
        assertEquals(429, second.getStatus());
        assertEquals("Too Many Requests", second.getErrorMessage());
    }

    @Test
    void different_methods_have_independent_limiters() throws Throwable {
        ProceedingJoinPoint pjp1 = mockJoinPoint("com.example.TestClass.method1()");
        ProceedingJoinPoint pjp2 = mockJoinPoint("com.example.TestClass.method2()");
        RateLimit ann1 = mockAnnotation(1000.0);
        RateLimit ann2 = mockAnnotation(1000.0);

        MockHttpServletResponse res = new MockHttpServletResponse();
        setRequestContext(res);

        Object r1 = aspect.around(pjp1, ann1);
        Object r2 = aspect.around(pjp2, ann2);

        assertEquals("ok", r1);
        assertEquals("ok", r2);
        verify(pjp1, times(1)).proceed();
        verify(pjp2, times(1)).proceed();
    }

    @Test
    void returns_null_gracefully_when_no_request_context() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint("com.example.TestClass.noContextMethod()");
        RateLimit annotation = mockAnnotation(0.0001);

        // 先消耗令牌（有 context）
        MockHttpServletResponse res = new MockHttpServletResponse();
        setRequestContext(res);
        aspect.around(pjp, annotation);

        // 无 context 时触发限流，应返回 null 不抛异常
        RequestContextHolder.resetRequestAttributes();
        Object result = aspect.around(pjp, annotation);

        assertNull(result);
    }
}
