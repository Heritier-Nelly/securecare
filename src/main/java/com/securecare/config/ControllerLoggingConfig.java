package com.securecare.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ControllerLoggingConfig implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(ControllerLoggingConfig.class);
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(new RequestLogger()); }
    private static final class RequestLogger implements HandlerInterceptor {
        @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            log.info("HTTP request started method={} path={} userId={}", request.getMethod(), request.getRequestURI(), userId());
            return true;
        }
        @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            if (ex != null) log.error("HTTP request failed method={} path={} userId={}", request.getMethod(), request.getRequestURI(), userId(), ex);
            else log.info("HTTP request completed method={} path={} status={} userId={}", request.getMethod(), request.getRequestURI(), response.getStatus(), userId());
        }
        private String userId() { var auth=SecurityContextHolder.getContext().getAuthentication(); return auth==null?"anonymous":String.valueOf(auth.getDetails()); }
    }
}
