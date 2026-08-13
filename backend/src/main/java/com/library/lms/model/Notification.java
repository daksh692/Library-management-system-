package com.library.lms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * A message addressed to one patron.
 *
 * <p>Persisted rather than fired-and-forgotten so the bell menu has history and
 * so a delivery failure (email bounce) never loses the message.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "user_read_created", def = "{'userId': 1, 'read': 1, 'createdAt': -1}")
})
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    /** Mongo {@code _id} of the recipient user. */
    @Indexed
    private String userId;

    /** READY_FOR_PICKUP, HOLD_EXPIRED, DUE_SOON, OVERDUE, ISSUED, QUEUED, PENALTY. */
    private String type;

    private String title;

    private String message;

    /** Deep link target, e.g. {@code /book/653f...}. Nullable. */
    private String link;

    @Builder.Default
    private boolean read = false;

    @Builder.Default
    private Date createdAt = new Date();

    /** Set once an email was successfully dispatched; null in Phase A. */
    private Date emailedAt;
}
