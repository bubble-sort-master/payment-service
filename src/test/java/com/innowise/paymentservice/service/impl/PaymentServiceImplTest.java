package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.PaymentStatus;
import com.innowise.paymentservice.event.PaymentEvent;
import com.innowise.paymentservice.exception.PaymentEventPublishingException;
import com.innowise.paymentservice.exception.PaymentProcessingException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private PaymentMapper paymentMapper;

  @Mock
  private RandomNumberClient randomNumberClient;

  @Mock
  private KafkaProducerService kafkaProducerService;

  @InjectMocks
  private PaymentServiceImpl paymentService;

  private final CreatePaymentRequest validRequest = new CreatePaymentRequest("order-1", 1L, BigDecimal.valueOf(100.00));
  private final LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
  private final LocalDateTime to = LocalDateTime.of(2025, 12, 31, 23, 59);

  @Test
  void create_shouldReturnSuccessWhenRandomIsEven() {
    Payment paymentEntity = new Payment();

    Payment savedEntity = new Payment();
    savedEntity.setOrderId("order-1");
    savedEntity.setUserId(1L);
    savedEntity.setStatus(PaymentStatus.SUCCESS);
    savedEntity.setTimestamp(LocalDateTime.now());

    PaymentResponse response = new PaymentResponse(
            "id",
            "order-1",
            1L,
            PaymentStatus.SUCCESS,
            savedEntity.getTimestamp(),
            BigDecimal.valueOf(100.00),
            null,
            null
    );

    when(randomNumberClient.getRandomNumber()).thenReturn(2);
    when(paymentMapper.toEntity(validRequest)).thenReturn(paymentEntity);
    when(paymentRepository.save(any(Payment.class))).thenReturn(savedEntity);
    when(paymentMapper.toDto(savedEntity)).thenReturn(response);

    PaymentResponse result = paymentService.create(validRequest);

    assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(paymentCaptor.capture());

    Payment capturedPayment = paymentCaptor.getValue();
    assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(capturedPayment.getTimestamp()).isNotNull();

    ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
    verify(kafkaProducerService).sendPaymentEvent(eventCaptor.capture());

    PaymentEvent event = eventCaptor.getValue();
    assertThat(event.orderId()).isEqualTo("order-1");
    assertThat(event.status()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(event.timestamp()).isNotNull();
  }

  @Test
  void create_shouldReturnFailedWhenRandomIsOdd() {
    Payment paymentEntity = new Payment();
    Payment savedEntity = new Payment();
    PaymentResponse response = new PaymentResponse("id", "order-2", 2L, PaymentStatus.FAILED, LocalDateTime.now(), BigDecimal.valueOf(50.00), null, null);

    when(randomNumberClient.getRandomNumber()).thenReturn(3);
    when(paymentMapper.toEntity(validRequest)).thenReturn(paymentEntity);
    when(paymentRepository.save(any(Payment.class))).thenReturn(savedEntity);
    when(paymentMapper.toDto(savedEntity)).thenReturn(response);

    PaymentResponse result = paymentService.create(validRequest);

    assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(paymentCaptor.capture());
    Payment capturedPayment = paymentCaptor.getValue();
    assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
  }

  @Test
  void create_shouldThrowPaymentProcessingExceptionWhenRandomServiceFails() {
    when(randomNumberClient.getRandomNumber()).thenThrow(new PaymentProcessingException("Service down"));

    assertThatThrownBy(() -> paymentService.create(validRequest))
            .isInstanceOf(PaymentProcessingException.class)
            .hasMessageContaining("Service down");

    verify(paymentRepository, never()).save(any());
  }

  @Test
  void getPayments_shouldReturnPaymentsFilteredByUserId() {
    Payment payment = new Payment();
    PaymentResponse response = mock(PaymentResponse.class);
    when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(List.of(payment));
    when(paymentMapper.toDto(payment)).thenReturn(response);

    List<PaymentResponse> result = paymentService.getPayments(1L, null, null);

    assertThat(result).hasSize(1);
    verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
  }

  @Test
  void getPayments_shouldReturnEmptyListWhenNoFiltersProvided() {
    List<PaymentResponse> result = paymentService.getPayments(null, null, null);

    assertThat(result).isEmpty();
    verify(mongoTemplate, never()).find(any(Query.class), eq(Payment.class));
  }

  @Test
  void getPayments_shouldFilterByOrderIdAndStatus() {
    Payment payment = new Payment();
    PaymentResponse response = mock(PaymentResponse.class);
    when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(List.of(payment));
    when(paymentMapper.toDto(payment)).thenReturn(response);

    List<PaymentResponse> result = paymentService.getPayments(null, "order-1", "success");

    assertThat(result).hasSize(1);
    verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
  }

  @Test
  void getTotalSumForDateRangeForUser_shouldReturnSum() {
    long totalCents = 50000L;
    PaymentServiceImpl.TotalSumResult mockResult = mock(PaymentServiceImpl.TotalSumResult.class);
    when(mockResult.total()).thenReturn(totalCents);

    AggregationResults<PaymentServiceImpl.TotalSumResult> aggResult = mock(AggregationResults.class);
    when(aggResult.getUniqueMappedResult()).thenReturn(mockResult);
    when(mongoTemplate.aggregate(any(Aggregation.class), eq(Payment.class), eq(PaymentServiceImpl.TotalSumResult.class)))
            .thenReturn(aggResult);

    BigDecimal sum = paymentService.getTotalSumForDateRangeForUser(1L, from, to);

    assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(500.00));
  }

  @Test
  void getTotalSumForDateRangeForUser_shouldReturnZeroWhenNoPayments() {
    AggregationResults<PaymentServiceImpl.TotalSumResult> aggResult = mock(AggregationResults.class);
    when(aggResult.getUniqueMappedResult()).thenReturn(null);
    when(mongoTemplate.aggregate(any(Aggregation.class), eq(Payment.class), eq(PaymentServiceImpl.TotalSumResult.class)))
            .thenReturn(aggResult);

    BigDecimal sum = paymentService.getTotalSumForDateRangeForUser(1L, from, to);

    assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void getTotalSumForDateRangeForAllUsers_shouldReturnSum() {
    long totalCents = 120000L;
    PaymentServiceImpl.TotalSumResult mockResult = mock(PaymentServiceImpl.TotalSumResult.class);
    when(mockResult.total()).thenReturn(totalCents);

    AggregationResults<PaymentServiceImpl.TotalSumResult> aggResult = mock(AggregationResults.class);
    when(aggResult.getUniqueMappedResult()).thenReturn(mockResult);
    when(mongoTemplate.aggregate(any(Aggregation.class), eq(Payment.class), eq(PaymentServiceImpl.TotalSumResult.class)))
            .thenReturn(aggResult);

    BigDecimal sum = paymentService.getTotalSumForDateRangeForAllUsers(from, to);

    assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(1200.00));
  }

  @Test
  void create_shouldThrowPaymentEventPublishingExceptionWhenKafkaPublishingFails() {
    Payment paymentEntity = new Payment();

    Payment savedEntity = new Payment();
    savedEntity.setOrderId("order-1");
    savedEntity.setUserId(1L);
    savedEntity.setStatus(PaymentStatus.SUCCESS);
    savedEntity.setTimestamp(LocalDateTime.now());

    when(randomNumberClient.getRandomNumber()).thenReturn(2);
    when(paymentMapper.toEntity(validRequest)).thenReturn(paymentEntity);
    when(paymentRepository.save(any(Payment.class))).thenReturn(savedEntity);

    doThrow(new PaymentEventPublishingException(
            "Could not publish payment event",
            new RuntimeException("Kafka error")
    )).when(kafkaProducerService).sendPaymentEvent(any(PaymentEvent.class));

    assertThatThrownBy(() -> paymentService.create(validRequest))
            .isInstanceOf(PaymentEventPublishingException.class)
            .hasMessageContaining("Could not publish payment event");

    verify(paymentRepository).save(any(Payment.class));
    verify(kafkaProducerService).sendPaymentEvent(any(PaymentEvent.class));
    verify(paymentMapper, never()).toDto(any(Payment.class));
  }
}