package com.innowise.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.PaymentStatus;
import com.innowise.paymentservice.model.Money;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

  private static final WireMockServer wireMockServer = new WireMockServer(0);

  @Container
  static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4");

  @Container
  static final KafkaContainer kafka = new KafkaContainer(
          DockerImageName.parse("apache/kafka-native:3.8.0")
  ).withStartupTimeout(Duration.ofMinutes(3));

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private PaymentRepository paymentRepository;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", () ->
            "mongodb://" + mongoDBContainer.getHost() + ":" + mongoDBContainer.getMappedPort(27017) + "/test");
    registry.add("external.random-number.url", () -> "http://localhost:" + wireMockServer.port());
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  @BeforeAll
  static void startWireMock() {
    wireMockServer.start();
  }

  @AfterAll
  static void stopWireMock() {
    wireMockServer.stop();
  }

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    wireMockServer.resetAll();
    paymentRepository.deleteAll();
  }

  private void stubRandomNumber(int number) {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/random"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"number\": " + number + "}")));
  }

  private Payment createPayment(Long orderId, Long userId, PaymentStatus status, Money amount, LocalDateTime timestamp) {
    Payment payment = new Payment();
    payment.setOrderId(orderId);
    payment.setUserId(userId);
    payment.setStatus(status);
    payment.setPaymentAmount(amount);
    payment.setTimestamp(timestamp);
    return paymentRepository.save(payment);
  }

  @Test
  void createPayment_shouldSendKafkaEvent_whenKafkaIsAvailable() throws Exception {
    stubRandomNumber(4);

    CreatePaymentRequest request =
            new CreatePaymentRequest(1L, 10L, BigDecimal.valueOf(123));

    mockMvc.perform(MockMvcRequestBuilders.post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

    List<Payment> payments = paymentRepository.findAll();
    assertThat(payments).hasSize(1);
    assertThat(kafka.isRunning()).isTrue();
  }

  @Test
  void createPayment_shouldReturnSuccessWhenRandomIsEven() throws Exception {
    stubRandomNumber(4);

    CreatePaymentRequest request = new CreatePaymentRequest(1L, 1L, BigDecimal.valueOf(150.00));

    mockMvc.perform(MockMvcRequestBuilders.post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.amount").value(150.00));

    List<Payment> payments = paymentRepository.findAll();
    assertThat(payments).hasSize(1);
    assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
  }

  @Test
  void createPayment_shouldReturnFailedWhenRandomIsOdd() throws Exception {
    stubRandomNumber(5);

    CreatePaymentRequest request = new CreatePaymentRequest(2L, 2L, BigDecimal.valueOf(50.00));

    mockMvc.perform(MockMvcRequestBuilders.post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("FAILED"));
  }

  @Test
  void createPayment_whenExternalServiceUnavailable_shouldReturn503() throws Exception {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/random"))
            .willReturn(aResponse().withStatus(503)));

    CreatePaymentRequest request = new CreatePaymentRequest(3L, 3L, BigDecimal.TEN);

    mockMvc.perform(MockMvcRequestBuilders.post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isServiceUnavailable());
  }

  @Test
  void createPayment_invalidRequest_shouldReturn400() throws Exception {
    CreatePaymentRequest invalid = new CreatePaymentRequest(null, null, BigDecimal.valueOf(-10));

    mockMvc.perform(MockMvcRequestBuilders.post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void getPayments_shouldFilterByUserId() throws Exception {
    createPayment(1L, 100L, PaymentStatus.SUCCESS, Money.of(2000L), LocalDateTime.now());
    createPayment(2L, 100L, PaymentStatus.FAILED, Money.of(3000L), LocalDateTime.now());
    createPayment(3L, 200L, PaymentStatus.SUCCESS, Money.of(1000L), LocalDateTime.now());

    mockMvc.perform(MockMvcRequestBuilders.get("/api/payments?userId=100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void getPayments_shouldFilterByOrderIdAndStatus() throws Exception {
    createPayment(10L, 10L, PaymentStatus.SUCCESS, Money.zero(), LocalDateTime.now());
    createPayment(11L, 10L, PaymentStatus.FAILED, Money.zero(), LocalDateTime.now());

    mockMvc.perform(MockMvcRequestBuilders.get("/api/payments?orderId=10&status=SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].orderId").value(10));
  }

  @Test
  void getPayments_noFilter_shouldReturnEmptyList() throws Exception {
    createPayment(1L, 1L, PaymentStatus.SUCCESS, Money.zero(), LocalDateTime.now());

    mockMvc.perform(MockMvcRequestBuilders.get("/api/payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getTotalSumForAllUsers_shouldReturnCorrectSum() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    createPayment(1L, 1L, PaymentStatus.SUCCESS, Money.of(1000L), now);
    createPayment(2L, 2L, PaymentStatus.SUCCESS, Money.of(2000L), now);

    String from = now.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    String to = now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    mockMvc.perform(MockMvcRequestBuilders.get("/api/payments/total?from=" + from + "&to=" + to))
            .andExpect(status().isOk())
            .andExpect(content().string("30.00"));
  }

  @Test
  void getTotalSumForUser_shouldReturnCorrectSum() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    createPayment(1L, 1L, PaymentStatus.SUCCESS, Money.of(5000L), now);
    createPayment(2L, 1L, PaymentStatus.FAILED, Money.of(1000L), now);
    createPayment(3L, 2L, PaymentStatus.SUCCESS, Money.of(7000L), now);

    String from = now.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    String to = now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    mockMvc.perform(MockMvcRequestBuilders.get("/api/payments/total/user/1?from=" + from + "&to=" + to))
            .andExpect(status().isOk())
            .andExpect(content().string("60.00"));
  }

  @Test
  void getTotalSumForUser_noPayments_shouldReturnZero() throws Exception {
    String from = "2025-01-01T00:00:00";
    String to = "2025-12-31T23:59:59";

    mockMvc.perform(MockMvcRequestBuilders.get("/api/payments/total/user/999?from=" + from + "&to=" + to))
            .andExpect(status().isOk())
            .andExpect(content().string("0.00"));
  }
}