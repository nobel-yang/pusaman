package com.lab.ai.pusaman.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuthInterceptorTest {

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    @Test
    void allows_request_when_token_matches() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertTrue(spy.preHandle(requestWithBearer("secret123"), res, new Object()));
        assertEquals(200, res.getStatus());
    }

    @Test
    void rejects_when_token_mismatch() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(requestWithBearer("wrong"), res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_authorization_header_missing() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(new MockHttpServletRequest(), res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_no_bearer_prefix() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "secret123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(req, res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_admin_token_env_not_configured() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return null; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(requestWithBearer("anything"), res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_admin_token_env_is_empty() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return ""; }
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(requestWithBearer("anything"), res, new Object()));
        assertEquals(401, res.getStatus());
    }

    @Test
    void rejects_when_bearer_token_is_empty_string() throws Exception {
        AdminAuthInterceptor spy = new AdminAuthInterceptor() {
            @Override
            protected String getAdminToken() { return "secret123"; }
        };
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(spy.preHandle(req, res, new Object()));
        assertEquals(401, res.getStatus());
    }
}
