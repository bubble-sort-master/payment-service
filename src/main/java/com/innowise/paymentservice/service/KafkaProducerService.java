package com.innowise.paymentservice.service;

import com.innowise.paymentservice.event.PaymentEvent;
import com.innowise.paymentservice.exception.PaymentEventPublishingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

  private static final String TOPIC = "payment-events";
  private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

  public void sendPaymentEvent(PaymentEvent event) {
    try {
      kafkaTemplate.send(TOPIC, event.orderId(), event).get(10, TimeUnit.SECONDS);
      log.info("Payment event sent for order {}", event.orderId());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Failed to send payment event for order {}: interrupted", event.orderId(), e);
      throw new PaymentEventPublishingException("Could not publish payment event", e);
    } catch (Exception e) {
      log.error("Failed to send payment event for order {}: {}", event.orderId(), e.getMessage(), e);
      throw new PaymentEventPublishingException("Could not publish payment event", e);
    }
  }
}