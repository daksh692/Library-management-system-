package com.library.lms.service;

import com.library.lms.exception.BusinessRuleException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Payment;
import com.library.lms.model.Transaction;
import com.library.lms.repository.PaymentRepository;
import com.library.lms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Manages the financial ledger of the library.
 *
 * <p>Handles the creation, tracking, and settlement of library card fees,
 * late return fines, and damage/loss penalties.</p>
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

    public Payment createPayment(String userId, double amount, String reason, String type, String referenceId) {
        return paymentRepository.save(Payment.builder()
                .userId(userId)
                .amount(amount)
                .reason(reason)
                .type(type)
                .status("PENDING")
                .referenceId(referenceId)
                .build());
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    public List<Payment> getUserPayments(String userId) {
        return paymentRepository.findByUserId(userId);
    }

    public double getOutstandingDues(String userId) {
        return paymentRepository.findByUserIdAndStatus(userId, "PENDING").stream()
                .mapToDouble(Payment::getAmount)
                .sum();
    }

    public Payment settlePayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        return markAsPaid(payment);
    }

    public void settlePaymentByReferenceId(String referenceId) {
        paymentRepository.findByReferenceId(referenceId).ifPresent(this::markAsPaid);
    }

    private Payment markAsPaid(Payment payment) {
        if ("PAID".equals(payment.getStatus())) {
            throw new BusinessRuleException("This payment is already settled.", "PAYMENT_ALREADY_PAID");
        }

        payment.setStatus("PAID");
        payment.setPaidAt(new Date());
        
        if ("BOOK_FINE".equals(payment.getType()) && payment.getReferenceId() != null) {
            transactionRepository.findById(payment.getReferenceId()).ifPresent(txn -> {
                txn.setPenaltyPaid(true);
                transactionRepository.save(txn);
            });
        }

        return paymentRepository.save(payment);
    }

    public boolean hasPendingPayments(String userId) {
        return paymentRepository.existsByUserIdAndStatus(userId, "PENDING");
    }
}
