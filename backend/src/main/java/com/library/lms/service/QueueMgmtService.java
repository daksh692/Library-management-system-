package com.library.lms.service;

import com.library.lms.config.LibraryProperties;
import com.library.lms.model.Transaction;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Background maintenance for the reservation queue.
 */
@Service
@RequiredArgsConstructor
public class QueueMgmtService {

    private static final Logger log = LoggerFactory.getLogger(QueueMgmtService.class);

    private final TransactionRepository transactionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;   // WS-05
    private final LibraryProperties props;

    /**
     * Lapses holds that were not collected inside the pickup window and passes the
     * copy to whoever is next.
     *
     * <p>Interval comes from {@code app.hold.sweep-interval-ms}.</p>
     */
    @Scheduled(fixedRateString = "${app.hold.sweep-interval-ms}")
    public void sweepExpiredHolds() {
        List<Transaction> holds = transactionRepository.findByStatus("HELD_FOR_PICKUP");
        if (holds.isEmpty()) return;

        int expired = 0;
        for (Transaction hold : holds) {
            if (!transactionService.isHoldExpired(hold)) continue;

            hold.setStatus("CANCELLED_HOLD");
            transactionRepository.save(hold);
            expired++;

            bookRepository.findById(hold.getBookId()).ifPresent(book ->
                    userRepository.findById(hold.getUserId()).ifPresent(user ->
                            notificationService.notifyHoldExpired(user, book)));

            transactionService.advanceQueue(hold.getBookId());
        }

        if (expired > 0) {
            log.info("Hold sweep: {} of {} holds lapsed", expired, holds.size());
        }
    }

    /**
     * Daily 09:00 reminder for anything due within the next two days.
     * Gives the overdue-fine machinery a chance to never trigger.
     */
    @Scheduled(cron = "${app.reminders.cron:0 0 9 * * *}")
    public void sendDueDateReminders() {
        notificationService.sendDueSoonReminders();
    }
}
