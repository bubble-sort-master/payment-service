package com.innowise.paymentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  private static HttpInputMessage emptyHttpInputMessage() {
    return new HttpInputMessage() {
      @Override
      public InputStream getBody() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
      }

      @Override
      public HttpHeaders getHeaders() {
        return new HttpHeaders();
      }
    };
  }

  @Test
  void handleMalformedJson_shouldReturnBadRequest() {
    HttpMessageNotReadableException exception =
            new HttpMessageNotReadableException("Bad JSON", emptyHttpInputMessage());

    ResponseEntity<String> response = handler.handleMalformedJson(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo("Malformed request body");
  }

  @Test
  void handleIllegalArgument_shouldReturnBadRequest() {
    IllegalArgumentException exception =
            new IllegalArgumentException("Invalid amount");

    ResponseEntity<String> response = handler.handleIllegalArgument(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo("Invalid amount");
  }

  @Test
  void handlePaymentEventPublishing_shouldReturnServiceUnavailable() {
    PaymentEventPublishingException exception =
            new PaymentEventPublishingException(
                    "Could not publish payment event",
                    new RuntimeException("Kafka error")
            );

    ResponseEntity<String> response = handler.handlePaymentEventPublishing(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(503);
    assertThat(response.getBody())
            .isEqualTo("Payment event could not be published. Please try again later.");
  }

  @Test
  void handlePaymentProcessing_shouldReturnServiceUnavailable() {
    PaymentProcessingException exception =
            new PaymentProcessingException("External service unavailable");

    ResponseEntity<String> response = handler.handlePaymentProcessing(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(503);
    assertThat(response.getBody()).isEqualTo("Payment processing is temporarily unavailable");
  }

  @Test
  void handleGenericException_shouldReturnInternalServerError() {
    RuntimeException exception = new RuntimeException("Unexpected error");

    ResponseEntity<String> response = handler.handleGenericException(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isEqualTo("Internal server error");
  }
}