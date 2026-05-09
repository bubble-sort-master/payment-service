package com.innowise.paymentservice.event;

import com.innowise.paymentservice.entity.PaymentStatus;
import org.springframework.modulith.events.Externalized;

import java.time.LocalDateTime;

@Externalized("payment-events")
public record PaymentEvent(
        String eventType,
        Long orderId,
        PaymentStatus status,
        LocalDateTime timestamp
) {
    public static final String TYPE_CREATE_PAYMENT = "CREATE_PAYMENT";
}