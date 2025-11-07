package com.example.graphbank.backend.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IdempotencyInterceptor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!request.getMethod().equals("POST")) return true;

        String key = request.getHeader("Idempotency-Key");
        if (key == null) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Idempotency-Key required");
            return false;
        }

        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            CachedResponse parsed = objectMapper.readValue(cached, CachedResponse.class);
            response.setStatus(parsed.status);
            response.getWriter().write(objectMapper.writeValueAsString(parsed.body));
            return false;
        }

        // Mark as processing
        CachedResponse processing = new CachedResponse(HttpStatus.PROCESSING.value(), new ProcessingBody("processing"));
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(processing), 30, TimeUnit.SECONDS);

        request.setAttribute("idempotencyKey", key);
        return true;
    }

    // Inner classes
    public static class CachedResponse {
        public int status;
        public Object body;

        public CachedResponse(int status, Object body) {
            this.status = status;
            this.body = body;
        }
    }

    public static class ProcessingBody {
        public String message;

        public ProcessingBody(String message) {
            this.message = message;
        }
    }
}