package com.innowise.paymentservice.config;

import com.innowise.paymentservice.converter.LongToMoneyConverter;
import com.innowise.paymentservice.converter.MoneyToLongConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

  @Bean
  public MongoCustomConversions customConversions() {
    return MongoCustomConversions.create(config -> config.registerConverters(Arrays.asList(
            new MoneyToLongConverter(),
            new LongToMoneyConverter()
    )));
  }
}