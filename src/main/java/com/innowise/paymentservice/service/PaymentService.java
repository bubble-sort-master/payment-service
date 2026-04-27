package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for processing payments.
 * <p>
 * Provides operations for creating a payment, retrieving a list of payments,
 * and calculating total amounts for a given time period.
 */
public interface PaymentService {

  /**
   * Creates a new payment.
   * <p>
   * Retrieves a random number from an external service and uses it to determine
   * the payment status ({@code SUCCESS} for even numbers, {@code FAILED} for odd).
   * The payment is persisted and a corresponding event is sent to Kafka.
   *
   * @param request the data for the payment to be created (must not be null)
   * @return response containing the details of the created payment
   * @throws org.springframework.web.client.RestClientException if the external random-number service is unreachable
   * @throws org.springframework.kafka.KafkaException if the payment event could not be sent to Kafka
   */
  PaymentResponse create(CreatePaymentRequest request);

  /**
   * Returns a list of payments filtered by the given criteria.
   * <p>
   * Filtering is optional; any combination of {@code userId}, {@code orderId},
   * and {@code status} may be provided. If all filters are {@code null}, an
   * empty list is returned.
   *
   * @param userId  the user ID (maybe null)
   * @param orderId the order ID (maybe null)
   * @param status  the payment status, e.g. {@code "SUCCESS"} or {@code "FAILED"} (maybe null)
   * @return list of payments matching the filters; never null
   */
  List<PaymentResponse> getPayments(Long userId, Long orderId, String status);

  /**
   * Calculates the total sum of successful payments for a specific user
   * within the given inclusive date range.
   *
   * @param userId the user ID
   * @param from   the start of the time range (inclusive)
   * @param to     the end of the time range (inclusive)
   * @return the total amount as a decimal; {@code 0.00} if no payments are found
   */
  BigDecimal getTotalSumForDateRangeForUser(Long userId, LocalDateTime from, LocalDateTime to);

  /**
   * Calculates the total sum of successful payments for all users
   * within the given inclusive date range.
   *
   * @param from the start of the time range (inclusive)
   * @param to   the end of the time range (inclusive)
   * @return the total amount as a decimal; {@code 0.00} if no payments are found
   */
  BigDecimal getTotalSumForDateRangeForAllUsers(LocalDateTime from, LocalDateTime to);
}