package com.library.lms.service;

import com.library.lms.config.LibraryProperties;
import com.library.lms.model.Book;
import com.library.lms.model.Notification;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.repository.NotificationRepository;
import com.library.lms.repository.TransactionRepository;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Creates patron notifications and, in Phase B, dispatches them by email.
 *
 * <p>Every method is best-effort: a notification failure must never roll back or
 * abort the library operation that triggered it. That is why each public method
 * swallows its own exceptions after logging.</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final TransactionRepository transactionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LibraryProperties props;
    private final EmailSender emailSender;      // no-op in Phase A, see §4

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    /** The reserved copy has arrived and the pickup clock is running. */
    public void notifyReadyForPickup(User user, Book book, int windowHours) {
        send(user, "READY_FOR_PICKUP",
                "Your reserved book is ready",
                String.format("'%s' is waiting for you at %s. Please collect it within %d hours.",
                        book.getName(), book.getLocation(), windowHours),
                "/book/" + book.getId(),
                true);   // important enough to email
    }

    public void notifyHoldExpired(User user, Book book) {
        send(user, "HOLD_EXPIRED",
                "Your reservation has lapsed",
                String.format("The pickup window for '%s' has passed and it has been offered "
                        + "to the next person in the queue. You are welcome to reserve it again.",
                        book.getName()),
                "/book/" + book.getId(),
                true);
    }

    public void notifyIssued(User user, Book book, Date dueDate) {
        send(user, "ISSUED",
                "Book issued",
                String.format("'%s' is yours until %s.",
                        book.getName(), formatDate(dueDate)),
                "/book/" + book.getId(),
                false);
    }

    public void notifyQueued(User user, Book book, Integer position) {
        send(user, "QUEUED",
                "You are in the queue",
                String.format("No copies of '%s' are free. You are number %d in line and "
                        + "will be notified when one becomes available.",
                        book.getName(), position),
                "/book/" + book.getId(),
                false);
    }

    public void notifyPenalty(User user, Book book, double amount) {
        send(user, "PENALTY",
                "A fine has been applied",
                String.format("A fine of $%.2f has been added to your account for '%s'. "
                        + "Please settle it at the front desk.", amount, book.getName()),
                null,
                true);
    }

    // ------------------------------------------------------------------
    // Scheduled reminders — called from QueueMgmtService
    // ------------------------------------------------------------------

    /**
     * Warns about anything due in the next two days, and chases anything already
     * overdue. Runs daily.
     */
    public void sendDueSoonReminders() {
        Date twoDaysOut = Date.from(Instant.now().plus(2, ChronoUnit.DAYS));
        Date now = new Date();
        int warned = 0, chased = 0;

        for (Transaction txn : transactionRepository.findByStatus("ISSUED")) {
            if (txn.getDueDate() == null) continue;

            User user = userRepository.findById(txn.getUserId()).orElse(null);
            Book book = bookRepository.findById(txn.getBookId()).orElse(null);
            if (user == null || book == null) continue;

            if (txn.getDueDate().before(now)) {
                long daysLate = ChronoUnit.DAYS.between(txn.getDueDate().toInstant(), Instant.now());
                double running = daysLate * props.getPenalty().getPerDayLate();

                send(user, "OVERDUE",
                        "Overdue: " + book.getName(),
                        String.format("'%s' was due on %s — %d day%s ago. A fine of $%.2f has "
                                + "accrued so far and grows by $%.2f each day.",
                                book.getName(), formatDate(txn.getDueDate()), daysLate,
                                daysLate == 1 ? "" : "s", running,
                                props.getPenalty().getPerDayLate()),
                        null, true);
                chased++;

            } else if (txn.getDueDate().before(twoDaysOut)) {
                send(user, "DUE_SOON",
                        "Due soon: " + book.getName(),
                        String.format("'%s' is due back on %s.",
                                book.getName(), formatDate(txn.getDueDate())),
                        "/book/" + book.getId(), false);
                warned++;
            }
        }

        if (warned + chased > 0) {
            log.info("Reminder run: {} due-soon, {} overdue", warned, chased);
        }
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    public List<Notification> forUser(String userObjectId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userObjectId);
    }

    public long unreadCount(String userObjectId) {
        return notificationRepository.countByUserIdAndReadFalse(userObjectId);
    }

    public void markRead(String notificationId, String userObjectId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userObjectId))   // never expose another patron's
                .ifPresent(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    public void markAllRead(String userObjectId) {
        List<Notification> unread =
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userObjectId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // ------------------------------------------------------------------

    /**
     * Persists the notification, then optionally emails it.
     *
     * @param alsoEmail whether this type warrants an email as well as an in-app entry
     */
    private void send(User user, String type, String title, String message,
                      String link, boolean alsoEmail) {
        if (user == null) return;

        try {
            Notification saved = notificationRepository.save(Notification.builder()
                    .userId(user.getId())
                    .type(type)
                    .title(title)
                    .message(message)
                    .link(link)
                    .build());

            if (alsoEmail && user.getEmail() != null && !user.getEmail().isBlank()) {
                boolean sent = emailSender.send(user.getEmail(), title, message);
                if (sent) {
                    saved.setEmailedAt(new Date());
                    notificationRepository.save(saved);
                }
            }
        } catch (Exception ex) {
            // Never let a notification failure break the library operation that caused it.
            log.error("Failed to deliver '{}' notification to {}", type, user.getUserId(), ex);
        }
    }

    private String formatDate(Date date) {
        if (date == null) return "an unknown date";
        return new java.text.SimpleDateFormat("d MMMM yyyy").format(date);
    }
}
