package com.tour.paymentservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tour.paymentservice.entities.Payment;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(String orderId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(String orderId);

    Payment findByTransactionId(String transactionId);
}