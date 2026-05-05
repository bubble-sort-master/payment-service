package com.innowise.paymentservice.event;

import com.innowise.paymentservice.entity.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentEvent(
        Long orderId,
        PaymentStatus status,
        LocalDateTime timestamp
) {}