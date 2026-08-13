package com.library.lms.repository;

import com.library.lms.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByUserId(String userId);
    List<Payment> findByStatus(String status);
    List<Payment> findByUserIdAndStatus(String userId, String status);
    boolean existsByUserIdAndStatus(String userId, String status);
    Optional<Payment> findByReferenceId(String referenceId);
}
