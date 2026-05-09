package com.innowise.paymentservice.converter;

import com.innowise.paymentservice.model.Money;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class LongToMoneyConverter implements Converter<Long, Money> {
  @Override
  public Money convert(Long source) {
    return source == null ? null : Money.of(source);
  }
}