package com.innowise.paymentservice.exception;

public class PaymentProcessingException extends RuntimeException {
  public PaymentProcessingException(String message) {
    super(message);
  }
}