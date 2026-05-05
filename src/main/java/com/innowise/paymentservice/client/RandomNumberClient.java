package com.innowise.paymentservice.client;

import com.innowise.paymentservice.dto.external.RandomNumberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "random-number-service",
        url = "${external.random-number.url}",
        fallbackFactory = RandomNumberClientFallbackFactory.class
)
public interface RandomNumberClient {
  @GetMapping("/api/random")
  RandomNumberResponse getRandomNumber();
}