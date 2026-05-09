package com.innowise.paymentservice.client;

import com.innowise.paymentservice.exception.PaymentProcessingException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RandomNumberClientFallbackFactory implements FallbackFactory<RandomNumberClient> {

  @Override
  public RandomNumberClient create(Throwable cause) {
    return () -> {
      handleException(cause);
      return null;
    };
  }

  private void handleException(Throwable cause) {
    FeignException feignException = unwrapFeignException(cause);

    if (feignException != null) {
      int status = feignException.status();
      if (status >= 400 && status < 500) {
        throw new PaymentProcessingException("External random service returned client error: " + status);
      }
      if (status >= 500) {
        throw new PaymentProcessingException("External random service server error: " + status);
      }
    }

    if (cause instanceof CallNotPermittedException ||
            (cause.getCause() instanceof CallNotPermittedException)) {
      throw new PaymentProcessingException("Random number service circuit breaker is OPEN");
    }

    throw new PaymentProcessingException("Random number service is unavailable: " + cause.getMessage());
  }

  private FeignException unwrapFeignException(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof FeignException fe) {
        return fe;
      }
      current = current.getCause();
    }
    return null;
  }
}