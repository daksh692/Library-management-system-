package com.library.lms.controller;

import com.library.lms.dto.response.PaymentResponse;
import com.library.lms.model.Payment;
import com.library.lms.repository.UserRepository;
import com.library.lms.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(@RequestParam(required = false) String status) {
        List<Payment> payments;
        if (status != null && !status.isBlank()) {
            payments = paymentService.getPaymentsByStatus(status.toUpperCase());
        } else {
            payments = paymentService.getAllPayments();
        }

        List<PaymentResponse> responses = payments.stream()
                .map(this::enrich)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<PaymentResponse> settlePayment(@PathVariable String id) {
        return ResponseEntity.ok(enrich(paymentService.settlePayment(id)));
    }

    private PaymentResponse enrich(Payment payment) {
        return PaymentResponse.of(
                payment,
                userRepository.findById(payment.getUserId()).orElse(null)
        );
    }
}
