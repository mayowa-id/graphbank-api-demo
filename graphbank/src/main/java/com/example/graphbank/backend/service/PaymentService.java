package com.example.graphbank.backend.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentService {

    private final ConcurrentHashMap<String, Integer> paymentLog = new ConcurrentHashMap<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public PaymentResult processPayment(String idempotencyKey, double amount, String accountId) {
        boolean isNew = paymentLog.putIfAbsent(idempotencyKey, 1) == null;

        if (isNew) {
            processedCount.incrementAndGet();
        }

        return new PaymentResult(
                "pay_" + System.nanoTime(),
                "confirmed",
                amount,
                accountId,
                isNew ? "processed" : "already_processed"
        );
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}

