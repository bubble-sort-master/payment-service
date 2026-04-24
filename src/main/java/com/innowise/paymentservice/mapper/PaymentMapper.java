package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.model.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "timestamp", ignore = true)
  @Mapping(target = "paymentAmount", source = "amount", qualifiedByName = "bigDecimalToMoney")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Payment toEntity(CreatePaymentRequest request);

  @Mapping(target = "amount", source = "paymentAmount", qualifiedByName = "moneyToBigDecimal")
  PaymentResponse toDto(Payment payment);

  @Named("bigDecimalToMoney")
  default Money bigDecimalToMoney(BigDecimal amount) {
    return Money.of(amount);
  }

  @Named("moneyToBigDecimal")
  default BigDecimal moneyToBigDecimal(Money money) {
    return money == null ? null : money.toBigDecimal();
  }
}