package com.innowise.paymentservice.exception;

public class PaymentEventPublishingException extends RuntimeException {
  public PaymentEventPublishingException(String message, Throwable cause) {
    super(message, cause);
  }
}