package com.library.lms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    private String bookId;

    private String userId;

    private Date issueDate; // Nullable if booked

    private Date dueDate; // Nullable if booked

    private Date returnDate; // Nullable

    private String status; // ISSUED, RETURNED, BOOKED_IN_QUEUE, HELD_FOR_PICKUP

    private String bookConditionOnReturn; // GOOD, DAMAGED, LOST, Nullable

    @Builder.Default
    private Double penaltyApplied = 0.0;

    @Builder.Default
    private boolean penaltyPaid = false;

    private Integer queueSequence; // Used if BOOKED_IN_QUEUE
}
