package com.library.lms.dto.response;

import com.library.lms.model.Payment;
import com.library.lms.model.User;

import java.util.Date;

public record PaymentResponse(
        String id,
        String userId,
        String userName,
        double amount,
        String reason,
        String type,
        String status,
        String referenceId,
        Date createdAt,
        Date paidAt
) {
    public static PaymentResponse of(Payment payment, User user) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                user != null ? user.getName() : "Unknown User",
                payment.getAmount(),
                payment.getReason(),
                payment.getType(),
                payment.getStatus(),
                payment.getReferenceId(),
                payment.getCreatedAt(),
                payment.getPaidAt()
        );
    }
}
