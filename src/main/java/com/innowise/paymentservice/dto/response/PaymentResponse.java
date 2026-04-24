package com.innowise.paymentservice.dto.response;

import com.innowise.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String orderId,
        Long userId,
        PaymentStatus status,
        LocalDateTime timestamp,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}