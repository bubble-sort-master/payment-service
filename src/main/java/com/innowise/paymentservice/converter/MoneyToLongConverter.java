package com.innowise.paymentservice.converter;

import com.innowise.paymentservice.model.Money;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class MoneyToLongConverter implements Converter<Money, Long> {
  @Override
  public Long convert(Money source) {
    return source == null ? null : source.getAmountInCents();
  }
}