package com.innowise.paymentservice.entity;

import com.innowise.paymentservice.model.Money;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "payments")
@CompoundIndex(name = "status_timestamp_idx", def = "{ 'status' : 1, 'timestamp' : -1 }")
public class Payment {

  @Id
  private String id;

  @Indexed
  @Field("order_id")
  private String orderId;

  @Indexed
  @Field("user_id")
  private Long userId;

  @Indexed
  @Field("status")
  private PaymentStatus status;

  @Field("timestamp")
  private LocalDateTime timestamp;

  @Field("payment_amount")
  private Money paymentAmount;

  @CreatedDate
  @Field("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private LocalDateTime updatedAt;
}