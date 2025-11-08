package com.example.graphbank.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.graphbank.backend.interceptor.IdempotencyInterceptor;
import com.example.graphbank.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.graphbank.backend.service.PaymentService;
import com.example.graphbank.backend.service.PaymentResult;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentController(PaymentService paymentService, RedisTemplate<String, String> redisTemplate) {
        this.paymentService = paymentService;
        this.redisTemplate = redisTemplate;
    }
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) throws Exception {

        String key = (String) httpRequest.getAttribute("idempotencyKey");

        var result = paymentService.processPayment(key, request.amount(), request.accountId());

        PaymentResponse response = new PaymentResponse(
                result.id(),
                result.status(),
                result.amount(),
                result.processingStatus()
        );

        // Safe Redis caching
        try {
            var cached = new IdempotencyInterceptor.CachedResponse(HttpStatus.CREATED.value(), response);
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(cached),
                    1, TimeUnit.HOURS
            );
        } catch (Exception e) {
            // Log but don't crash
            System.err.println("Redis cache failed: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    // Debug: Prove only one payment was processed
    @GetMapping("/debug/processed-count")
    public int getProcessedCount() {
        return paymentService.getProcessedCount();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }


}


// Request/Response DTOs using records (cleaner, immutable)
record PaymentRequest(double amount, String accountId) {}

record PaymentResponse(
        String id,
        String status,
        double amount,
        String processingStatus  // "processed" or "already_processed"
) {}