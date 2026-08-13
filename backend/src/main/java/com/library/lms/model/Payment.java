package com.library.lms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Indexed
    private String userId;

    private double amount;

    private String reason; // e.g., "Library Card Issue", "Lost Book Fine"

    private String type; // e.g., "CARD_FEE", "BOOK_FINE"

    private String status; // "PENDING" or "PAID"

    private String referenceId; // Transaction ID if applicable

    @Builder.Default
    private Date createdAt = new Date();

    private Date paidAt;
}
