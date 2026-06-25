package com.lab.ai.pusaman.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.validation.constraints.NotNull;

@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) throws Exception {
        String adminToken = getAdminToken();
        if (adminToken == null || adminToken.isEmpty()) {
            log.warn("Admin_token is null");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return false;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            log.warn("Authorization is illegal: {}", authorization);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return false;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (!adminToken.equals(token)) {
            log.warn("Authorization is not match. config:{}, request:{}", adminToken, authorization);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return false;
        }

        return true;
    }

    protected String getAdminToken() {
        String name = "ADMIN_TOKEN";
        String value = System.getProperty(name);
        if (value == null) {
            value = System.getenv(name);
        }
        return value;
    }
}
