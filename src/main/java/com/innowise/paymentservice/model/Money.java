package com.innowise.paymentservice.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money implements Comparable<Money> {

  private long amountInCents;

  private Money(long amountInCents) {
    if (amountInCents < 0) {
      throw new IllegalArgumentException("Amount can't be negative");
    }
    this.amountInCents = amountInCents;
  }

  public static Money of(long amountInCents) {
    return new Money(amountInCents);
  }

  public static Money of(BigDecimal amount) {
    return new Money(amount.multiply(BigDecimal.valueOf(100)).longValueExact());
  }

  public static Money zero() {
    return new Money(0);
  }

  public long getAmountInCents() {
    return amountInCents;
  }

  public BigDecimal toBigDecimal() {
    return BigDecimal.valueOf(amountInCents, 2);
  }

  public Money add(Money other) {
    return new Money(this.amountInCents + other.amountInCents);
  }

  public Money subtract(Money other) {
    return new Money(this.amountInCents - other.amountInCents);
  }

  public Money multiply(long multiplier) {
    return new Money(this.amountInCents * multiplier);
  }

  @Override
  public int compareTo(Money o) {
    return Long.compare(this.amountInCents, o.amountInCents);
  }

  @Override
  public String toString() {
    return toBigDecimal().setScale(2, RoundingMode.HALF_UP).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Money other)) return false;
    return amountInCents == other.amountInCents;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(amountInCents);
  }
}