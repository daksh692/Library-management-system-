package com.library.lms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    // the queue lookup in advanceQueue()
    @CompoundIndex(name = "book_status_seq",
                   def = "{'bookId': 1, 'status': 1, 'queueSequence': 1}"),
    // the duplicate-loan guard
    @CompoundIndex(name = "user_book",
                   def = "{'userId': 1, 'bookId': 1}"),
    // the hold-expiry sweep and the due-date reminder run
    @CompoundIndex(name = "status_due",
                   def = "{'status': 1, 'dueDate': 1}")
})
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    @Indexed
    private String bookId;

    @Indexed
    private String userId;

    private Date issueDate; // Nullable if booked

    private Date dueDate; // Nullable if booked

    private Date returnDate; // Nullable

    @Indexed
    private String status; // ISSUED, RETURNED, BOOKED_IN_QUEUE, HELD_FOR_PICKUP

    private String bookConditionOnReturn; // GOOD, DAMAGED, LOST, Nullable

    @Builder.Default
    private Double penaltyApplied = 0.0;

    @Builder.Default
    private boolean penaltyPaid = false;

    private Integer queueSequence; // Used if BOOKED_IN_QUEUE
}
