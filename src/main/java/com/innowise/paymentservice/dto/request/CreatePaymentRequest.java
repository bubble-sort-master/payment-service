package com.innowise.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "Order ID must not be null")
        String orderId,

        @NotNull(message = "User ID must not be null")
        Long userId,

        @NotNull(message = "Payment amount must not be null")
        @Positive(message = "Payment amount must be positive")
        BigDecimal amount
) {}