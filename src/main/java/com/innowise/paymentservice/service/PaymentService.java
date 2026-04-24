package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

  PaymentResponse create(CreatePaymentRequest request);

  List<PaymentResponse> getPayments(Long userId, String orderId, String status);

  BigDecimal getTotalSumForDateRangeForUser(Long userId, LocalDateTime from, LocalDateTime to);

  BigDecimal getTotalSumForDateRangeForAllUsers(LocalDateTime from, LocalDateTime to);
}