package com.marcos.usersservice.config.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class RequestTimingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "start_time";
    private static final long WARNING_REQUEST_MS = 50;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        log.info("➡️ {} - Request - {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME);
        if (startTime == null) return;

        long duration = System.currentTimeMillis() - startTime;
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        boolean isError = ex != null || status >= 500;
        String emoji = isError ? "❌" : (duration > WARNING_REQUEST_MS ? "🐌" : "✅");
        String message = "{} {} {} - {} - {}ms" + (isError ? " - ERROR" : "");

        (isError ? (Runnable) () -> log.error(message, emoji, method, uri, status, duration)
                : (Runnable) () -> log.info(message, emoji, method, uri, status, duration))
                .run();
    }
}
