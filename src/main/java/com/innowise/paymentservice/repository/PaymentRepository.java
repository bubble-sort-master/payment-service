package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
  List<Payment> findByUserId(Long userId);
  List<Payment> findByOrderId(Long orderId);
  List<Payment> findByStatus(String status);
}