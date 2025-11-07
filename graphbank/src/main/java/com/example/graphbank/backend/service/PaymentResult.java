package com.example.graphbank.backend.service;

// Internal result
public record PaymentResult(String id, String status, double amount, String accountId, String processingStatus) {
}
