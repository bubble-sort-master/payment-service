package com.innowise.paymentservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

  @Test
  void ofLong_shouldCreateMoneyInCents() {
    Money money = Money.of(1234L);

    assertThat(money.getAmountInCents()).isEqualTo(1234L);
    assertThat(money.toBigDecimal()).isEqualByComparingTo("12.34");
  }

  @Test
  void ofBigDecimal_shouldConvertAmountToCents() {
    Money money = Money.of(BigDecimal.valueOf(15.99));

    assertThat(money.getAmountInCents()).isEqualTo(1599L);
    assertThat(money.toBigDecimal()).isEqualByComparingTo("15.99");
  }

  @Test
  void ofBigDecimal_shouldThrowWhenMoreThanTwoDecimalPlaces() {
    assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(10.999)))
            .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void zero_shouldReturnZeroMoney() {
    Money money = Money.zero();

    assertThat(money.getAmountInCents()).isZero();
    assertThat(money.toBigDecimal()).isEqualByComparingTo("0.00");
  }

  @Test
  void negativeAmount_shouldThrowException() {
    assertThatThrownBy(() -> Money.of(-1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Amount can't be negative");
  }

  @Test
  void add_shouldReturnSum() {
    Money first = Money.of(1000L);
    Money second = Money.of(250L);

    Money result = first.add(second);

    assertThat(result.getAmountInCents()).isEqualTo(1250L);
    assertThat(result.toString()).isEqualTo("12.50");
  }

  @Test
  void subtract_shouldReturnDifference() {
    Money first = Money.of(1000L);
    Money second = Money.of(250L);

    Money result = first.subtract(second);

    assertThat(result.getAmountInCents()).isEqualTo(750L);
    assertThat(result.toString()).isEqualTo("7.50");
  }

  @Test
  void subtract_shouldThrowWhenResultIsNegative() {
    Money first = Money.of(100L);
    Money second = Money.of(200L);

    assertThatThrownBy(() -> first.subtract(second))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Amount can't be negative");
  }

  @Test
  void multiply_shouldReturnMultipliedMoney() {
    Money money = Money.of(500L);

    Money result = money.multiply(3);

    assertThat(result.getAmountInCents()).isEqualTo(1500L);
    assertThat(result.toString()).isEqualTo("15.00");
  }

  @Test
  void multiplyByNegative_shouldThrowException() {
    Money money = Money.of(500L);

    assertThatThrownBy(() -> money.multiply(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Amount can't be negative");
  }

  @Test
  void compareTo_shouldCompareMoneyAmounts() {
    Money smaller = Money.of(100L);
    Money bigger = Money.of(200L);
    Money same = Money.of(100L);

    assertThat(smaller.compareTo(bigger)).isLessThan(0);
    assertThat(bigger.compareTo(smaller)).isGreaterThan(0);
    assertThat(smaller.compareTo(same)).isZero();
  }

  @Test
  void equals_shouldReturnTrueForSameAmount() {
    Money first = Money.of(1000L);
    Money second = Money.of(1000L);

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  void equals_shouldReturnTrueForSameObject() {
    Money money = Money.of(1000L);

    assertThat(money).isEqualTo(money);
  }

  @Test
  void equals_shouldReturnFalseForDifferentAmount() {
    Money first = Money.of(1000L);
    Money second = Money.of(2000L);

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void equals_shouldReturnFalseForDifferentType() {
    Money money = Money.of(1000L);

    assertThat(money).isNotEqualTo("1000");
  }

  @Test
  void equals_shouldReturnFalseForNull() {
    Money money = Money.of(1000L);

    assertThat(money).isNotEqualTo(null);
  }

  @Test
  void toString_shouldReturnAmountWithTwoDecimalPlaces() {
    Money money = Money.of(1234L);

    assertThat(money.toString()).isEqualTo("12.34");
  }
}