package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.PaymentStatus;
import com.innowise.paymentservice.event.PaymentEvent;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Money;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final MongoTemplate mongoTemplate;
  private final PaymentMapper paymentMapper;
  private final RandomNumberClient randomNumberClient;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public PaymentResponse create(CreatePaymentRequest request) {
    Integer random = randomNumberClient.getRandomNumber().number();
    PaymentStatus status = (random % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

    Payment payment = paymentMapper.toEntity(request);
    payment.setStatus(status);
    payment.setTimestamp(LocalDateTime.now());
    Payment saved = paymentRepository.save(payment);

    eventPublisher.publishEvent(
            new PaymentEvent(PaymentEvent.TYPE_CREATE_PAYMENT, saved.getOrderId(), saved.getStatus(), saved.getTimestamp())
    );

    return paymentMapper.toDto(saved);
  }

  @Override
  public List<PaymentResponse> getPayments(Long userId, Long orderId, String status) {
    Criteria criteria = new Criteria();
    if (userId != null) {
      criteria.and("user_id").is(userId);
    }
    if (orderId != null) {
      criteria.and("order_id").is(orderId);
    }
    if (status != null) {
      criteria.and("status").is(status.toUpperCase());
    }

    Query query = Query.query(criteria);
    if (userId == null && orderId == null && status == null) {
      return List.of();
    }

    List<Payment> payments = mongoTemplate.find(query, Payment.class);
    return payments.stream().map(paymentMapper::toDto).toList();
  }

  @Override
  public BigDecimal getTotalSumForDateRangeForUser(Long userId, LocalDateTime from, LocalDateTime to) {
    var match = match(Criteria.where("user_id").is(userId)
            .and("status").is(PaymentStatus.SUCCESS.name())
            .and("timestamp").gte(from).lte(to));
    var group = group().sum("payment_amount").as("total");

    var agg = newAggregation(match, group);
    var result = mongoTemplate.aggregate(agg, Payment.class, TotalSumResult.class)
            .getUniqueMappedResult();

    return result != null ? Money.of(result.total()).toBigDecimal() : Money.zero().toBigDecimal();
  }

  @Override
  public BigDecimal getTotalSumForDateRangeForAllUsers(LocalDateTime from, LocalDateTime to) {
    var match = match(Criteria.where("status").is(PaymentStatus.SUCCESS.name())
            .and("timestamp").gte(from).lte(to));
    var group = group().sum("payment_amount").as("total");

    var agg = newAggregation(match, group);
    var result = mongoTemplate.aggregate(agg, Payment.class, TotalSumResult.class)
            .getUniqueMappedResult();

    return result != null ? Money.of(result.total()).toBigDecimal() : Money.zero().toBigDecimal();
  }

  static class TotalSumResult {
    private long total;
    public long total() { return total; }
    public void setTotal(long total) { this.total = total; }
  }
}