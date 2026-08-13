package com.library.lms.dto.response;

import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;

import java.util.Date;

/**
 * Transaction enriched with the book title and borrower name.
 *
 * <p>This is what makes the admin transactions table readable — the raw entity
 * carries only opaque ObjectIds. See WS-03 §2.</p>
 */
public record TransactionResponse(
        String id,
        String bookId,
        String bookName,
        String bookIsbn,
        String bookLocation,
        String bookAuthor,
        String bookPhotoUrl,
        String bookGenre,
        String userId,
        String userCode,
        String userName,
        Date issueDate,
        Date dueDate,
        Date returnDate,
        String status,
        String bookConditionOnReturn,
        Double penaltyApplied,
        boolean penaltyPaid,
        Integer queueSequence,
        Date holdExpiresAt,
        boolean overdue,
        long daysOverdue
) {
    /**
     * @param book may be null if the referenced book was hard-deleted; renders as "Unknown".
     * @param user may be null for the same reason.
     * @param holdWindowHours the configured pickup window, used to derive {@code holdExpiresAt}.
     */
    public static TransactionResponse of(Transaction txn, Book book, User user, int holdWindowHours) {
        Date now = new Date();

        boolean isOverdue = "ISSUED".equals(txn.getStatus())
                && txn.getDueDate() != null
                && txn.getDueDate().before(now);

        long overdueDays = 0;
        if (isOverdue) {
            long millis = now.getTime() - txn.getDueDate().getTime();
            overdueDays = millis / (1000L * 60 * 60 * 24);
        }

        Date holdExpiry = null;
        if ("HELD_FOR_PICKUP".equals(txn.getStatus()) && txn.getIssueDate() != null) {
            holdExpiry = new Date(txn.getIssueDate().getTime()
                    + (long) holdWindowHours * 60 * 60 * 1000);
        }

        return new TransactionResponse(
                txn.getId(),
                txn.getBookId(),
                book != null ? book.getName() : "Unknown book",
                book != null ? book.getIsbn() : null,
                book != null ? book.getLocation() : null,
                book != null ? book.getAuthor() : null,
                book != null ? book.getPhotoUrl() : null,
                book != null ? book.getGenre() : null,
                txn.getUserId(),
                user != null ? user.getUserId() : null,
                user != null ? user.getName() : "Unknown user",
                txn.getIssueDate(),
                txn.getDueDate(),
                txn.getReturnDate(),
                txn.getStatus(),
                txn.getBookConditionOnReturn(),
                txn.getPenaltyApplied(),
                txn.isPenaltyPaid(),
                txn.getQueueSequence(),
                holdExpiry,
                isOverdue,
                overdueDays
        );
    }
}
