package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller responsible for managing payments.
 * <p>
 * Provides endpoints for creating, retrieving and aggregating payments.
 * Delegates all business logic to {@link PaymentService}.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  /**
   * Creates a new payment.
   * <p>
   * Accepts payment details, validates input, delegates payment processing
   * to the service layer, and returns the created payment with its status.
   *
   * @param request DTO containing order ID, user ID and payment amount.
   * @return created payment including status (SUCCESS/FAILED) and timestamp.
   * @see PaymentService#create(CreatePaymentRequest)
   */
  @PostMapping
  public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(paymentService.create(request));
  }

  /**
   * Retrieves payments filtered by one or more optional criteria.
   * <p>
   * At least one filter parameter should be provided; if none are given,
   * an empty list is returned.
   *
   * @param userId  optional user identifier.
   * @param orderId optional order identifier.
   * @param status  optional payment status (e.g. SUCCESS, FAILED).
   * @return list of matching payments.
   * @see PaymentService#getPayments(Long, String, String)
   */
  @GetMapping
  public ResponseEntity<List<PaymentResponse>> getPayments(
          @RequestParam(required = false) Long userId,
          @RequestParam(required = false) String orderId,
          @RequestParam(required = false) String status) {
    return ResponseEntity.ok(paymentService.getPayments(userId, orderId, status));
  }

  /**
   * Returns the total sum of payments within a given date range for all users.
   * <p>
   * Intended for administrators.
   *
   * @param from start of the period (inclusive), ISO‑8601 format.
   * @param to   end of the period (inclusive), ISO‑8601 format.
   * @return total amount as {@link BigDecimal}.
   * @see PaymentService#getTotalSumForDateRangeForAllUsers(LocalDateTime, LocalDateTime)
   */
  @GetMapping("/total")
  public ResponseEntity<BigDecimal> getTotalSumForAllUsers(
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
    return ResponseEntity.ok(paymentService.getTotalSumForDateRangeForAllUsers(from, to));
  }

  /**
   * Returns the total sum of payments within a given date range for a specific user.
   * <p>
   * Typically called by the user themselves or by an administrator.
   *
   * @param userId user identifier.
   * @param from   start of the period (inclusive), ISO‑8601 format.
   * @param to     end of the period (inclusive), ISO‑8601 format.
   * @return total amount as {@link BigDecimal}.
   * @see PaymentService#getTotalSumForDateRangeForUser(Long, LocalDateTime, LocalDateTime)
   */
  @GetMapping("/total/user/{userId}")
  public ResponseEntity<BigDecimal> getTotalSumForUser(
          @PathVariable Long userId,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
    return ResponseEntity.ok(paymentService.getTotalSumForDateRangeForUser(userId, from, to));
  }
}