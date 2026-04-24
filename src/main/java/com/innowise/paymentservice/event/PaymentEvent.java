package com.innowise.paymentservice.event;

import com.innowise.paymentservice.entity.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentEvent(
        String orderId,
        PaymentStatus status,
        LocalDateTime timestamp
) {}