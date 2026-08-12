package com.library.lms.service;

import com.library.lms.config.LibraryProperties;
import com.library.lms.dto.ReturnRequest;
import com.library.lms.dto.TransactionRequest;
import com.library.lms.exception.BusinessRuleException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Lifecycle of every loan and reservation.
 *
 * <h2>Copy accounting invariant</h2>
 * A copy leaves {@code availableCopies} when it is issued and returns only when it is
 * given back <em>and</em> nobody is waiting. When a queue exists the copy passes
 * directly from the returning borrower to the next patron via {@code HELD_FOR_PICKUP},
 * never re-entering the available pool. This keeps
 * {@code availableCopies + issued + held == totalCopies} at all times.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    /** Statuses that occupy one of a patron's borrowing slots. */
    private static final Set<String> ACTIVE_STATUSES =
            Set.of("ISSUED", "HELD_FOR_PICKUP", "BOOKED_IN_QUEUE");

    private final TransactionRepository transactionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final SequenceGeneratorService sequenceGenerator;
    private final LibraryProperties props;
    private final NotificationService notificationService;   // WS-05

    // ------------------------------------------------------------------
    // Issue
    // ------------------------------------------------------------------

    /**
     * Issues a book, or enqueues the patron when no copy is free.
     *
     * @throws ResourceNotFoundException if the book or patron does not exist
     * @throws BusinessRuleException     if the card has expired, the borrowing limit is
     *                                   reached, or this patron already holds this title
     */
    // @Transactional (Disabled temporarily for non-replica set local dev)
    public Transaction issueBook(TransactionRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Book", request.getBookId()));

        User user = userRepository.findByUserId(request.getUserId())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        assertCardValid(user);
        assertNotDuplicate(user, book);
        assertUnderBorrowingLimit(user);

        Transaction transaction = Transaction.builder()
                .bookId(book.getId())
                .userId(user.getId())
                .build();

        Book reserved = bookRepository.tryReserveCopy(book.getId());

        if (reserved != null) {
            transaction.setStatus("ISSUED");
            transaction.setIssueDate(new Date());
            transaction.setDueDate(Date.from(
                    Instant.now().plus(props.getLoan().getPeriodDays(), ChronoUnit.DAYS)));

            log.info("Issued '{}' to {} (due {})",
                    book.getName(), user.getUserId(), transaction.getDueDate());
        } else {
            transaction.setStatus("BOOKED_IN_QUEUE");
            transaction.setQueueSequence(
                    (int) sequenceGenerator.nextValue("queue:" + book.getId()));

            log.info("Queued {} for '{}' at position {}",
                    user.getUserId(), book.getName(), transaction.getQueueSequence());
        }

        Transaction saved = transactionRepository.save(transaction);

        if ("BOOKED_IN_QUEUE".equals(saved.getStatus())) {
            notificationService.notifyQueued(user, book, saved.getQueueSequence());
        } else {
            notificationService.notifyIssued(user, book, saved.getDueDate());
        }

        return saved;
    }

    // ------------------------------------------------------------------
    // Handover — the transition that was missing entirely
    // ------------------------------------------------------------------

    /**
     * Completes a reservation: the patron has arrived within the pickup window and
     * physically collected the book.
     *
     * <p>No stock arithmetic happens here — the copy was withheld from the available
     * pool when the hold was created, so it simply changes hands.</p>
     *
     * @throws BusinessRuleException if the transaction is not currently held, or the
     *                               pickup window has already lapsed
     */
    // @Transactional (Disabled temporarily for non-replica set local dev)
    public Transaction handoverHeldBook(String transactionId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (!"HELD_FOR_PICKUP".equals(txn.getStatus())) {
            throw new BusinessRuleException(
                    "This reservation is not awaiting pickup — it is " + txn.getStatus() + ".",
                    "NOT_AWAITING_PICKUP");
        }

        if (isHoldExpired(txn)) {
            throw new BusinessRuleException(
                    "The " + props.getHold().getWindowHours()
                            + "-hour pickup window for this reservation has lapsed.",
                    "HOLD_EXPIRED");
        }

        txn.setStatus("ISSUED");
        txn.setIssueDate(new Date());
        txn.setDueDate(Date.from(
                Instant.now().plus(props.getLoan().getPeriodDays(), ChronoUnit.DAYS)));

        Transaction saved = transactionRepository.save(txn);

        bookRepository.findById(txn.getBookId()).ifPresent(book ->
                userRepository.findById(txn.getUserId()).ifPresent(user ->
                        notificationService.notifyIssued(user, book, saved.getDueDate())));

        log.info("Handed over held transaction {}", transactionId);
        return saved;
    }

    // ------------------------------------------------------------------
    // Return
    // ------------------------------------------------------------------

    /**
     * Processes a physical check-in: computes fines, records condition, records the
     * genre against the patron's reading profile, and advances the queue.
     */
    // @Transactional (Disabled temporarily for non-replica set local dev)
    public Transaction returnBook(ReturnRequest request) {
        Transaction txn = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction", request.getTransactionId()));

        if (!"ISSUED".equals(txn.getStatus())) {
            throw new BusinessRuleException(
                    "Only an issued book can be returned — this transaction is "
                            + txn.getStatus() + ".",
                    "TRANSACTION_NOT_ISSUED");
        }

        Book book = bookRepository.findById(txn.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", txn.getBookId()));

        Date now = new Date();
        txn.setStatus("RETURNED");
        txn.setReturnDate(now);
        txn.setBookConditionOnReturn(request.getCondition());
        txn.setPenaltyApplied(calculatePenalty(txn, book, request.getCondition(), now));
        txn.setPenaltyPaid(txn.getPenaltyApplied() == 0.0);

        transactionRepository.save(txn);
        recordGenreForRecommendations(txn.getUserId(), book.getGenre());

        if ("LOST".equals(request.getCondition())) {
            // The copy no longer exists. It never returns to the pool, and the
            // catalogue total shrinks. Anyone queued stays queued.
            bookRepository.writeOffCopy(book.getId());
            log.warn("Book '{}' written off as lost (transaction {})", book.getName(), txn.getId());
        } else {
            advanceQueue(book.getId());
        }

        if (txn.getPenaltyApplied() > 0) {
            userRepository.findById(txn.getUserId()).ifPresent(user ->
                    notificationService.notifyPenalty(user, book, txn.getPenaltyApplied()));
        }

        return txn;
    }

    // ------------------------------------------------------------------
    // Queue
    // ------------------------------------------------------------------

    /**
     * Gives the freed copy to the next patron in line, or returns it to the shelf.
     *
     * <p>Public only because {@link QueueMgmtService} calls it when a hold lapses.
     * Nothing else should — the copy-accounting invariant depends on it being
     * reached exactly once per freed copy.</p>
     */
    // @Transactional (Disabled temporarily for non-replica set local dev)
    public void advanceQueue(String bookId) {
        List<Transaction> queue = transactionRepository
                .findByBookIdAndStatus(bookId, "BOOKED_IN_QUEUE")
                .stream()
                .sorted(Comparator.comparing(
                        Transaction::getQueueSequence,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (queue.isEmpty()) {
            bookRepository.releaseCopy(bookId);
            return;
        }

        Transaction next = queue.get(0);
        next.setStatus("HELD_FOR_PICKUP");
        next.setIssueDate(new Date());   // marks the start of the pickup window
        transactionRepository.save(next);

        bookRepository.findById(bookId).ifPresent(book ->
                userRepository.findById(next.getUserId()).ifPresent(user ->
                        notificationService.notifyReadyForPickup(
                                user, book, props.getHold().getWindowHours())));

        log.info("Advanced queue for book {} — transaction {} is now held",
                bookId, next.getId());
    }

    /** @return true when the pickup window on a held transaction has lapsed. */
    public boolean isHoldExpired(Transaction txn) {
        if (txn.getIssueDate() == null) return false;
        long elapsedHours = ChronoUnit.HOURS.between(
                txn.getIssueDate().toInstant(), Instant.now());
        return elapsedHours >= props.getHold().getWindowHours();
    }

    // ------------------------------------------------------------------
    // Fines
    // ------------------------------------------------------------------

    /**
     * Marks a fine as settled.
     *
     * @throws BusinessRuleException if there is no outstanding fine
     */
    // @Transactional (Disabled temporarily for non-replica set local dev)
    public Transaction settlePenalty(String transactionId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (txn.getPenaltyApplied() == null || txn.getPenaltyApplied() == 0.0) {
            throw new BusinessRuleException(
                    "There is no outstanding fine on this transaction.", "NO_PENALTY_DUE");
        }
        if (txn.isPenaltyPaid()) {
            throw new BusinessRuleException(
                    "This fine has already been settled.", "PENALTY_ALREADY_PAID");
        }

        txn.setPenaltyPaid(true);
        log.info("Fine of {} settled on transaction {}", txn.getPenaltyApplied(), transactionId);
        return transactionRepository.save(txn);
    }

    /** @return total unpaid fines across every transaction for this patron. */
    public double outstandingFines(String userObjectId) {
        return transactionRepository.findByUserId(userObjectId).stream()
                .filter(t -> !t.isPenaltyPaid())
                .mapToDouble(t -> t.getPenaltyApplied() == null ? 0.0 : t.getPenaltyApplied())
                .sum();
    }

    private double calculatePenalty(Transaction txn, Book book, String condition, Date returnedAt) {
        double penalty = 0.0;
        LibraryProperties.Penalty policy = props.getPenalty();

        if (txn.getDueDate() != null && returnedAt.after(txn.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(
                    txn.getDueDate().toInstant(), returnedAt.toInstant());
            penalty += daysLate * policy.getPerDayLate();
        }

        double price = book.getPrice() != null ? book.getPrice() : policy.getDefaultBookPrice();
        if ("DAMAGED".equals(condition)) {
            penalty += price * policy.getDamagedRate();
        } else if ("LOST".equals(condition)) {
            penalty += price * policy.getLostRate();
        }

        return Math.round(penalty * 100.0) / 100.0;
    }

    // ------------------------------------------------------------------
    // Guards
    // ------------------------------------------------------------------

    private void assertCardValid(User user) {
        if (!props.getCard().isEnforceExpiry()) return;

        if (user.getCardEndDate() != null && user.getCardEndDate().before(new Date())) {
            throw new BusinessRuleException(
                    "This library card expired on " + user.getCardEndDate()
                            + ". Renew it before borrowing.",
                    "CARD_EXPIRED");
        }
    }

    private void assertNotDuplicate(User user, Book book) {
        boolean alreadyHolds = transactionRepository
                .findByUserIdAndBookId(user.getId(), book.getId())
                .stream()
                .anyMatch(t -> ACTIVE_STATUSES.contains(t.getStatus()));

        if (alreadyHolds) {
            throw new BusinessRuleException(
                    user.getName() + " already has an active loan or reservation for '"
                            + book.getName() + "'.",
                    "DUPLICATE_ACTIVE_LOAN");
        }
    }

    private void assertUnderBorrowingLimit(User user) {
        long active = transactionRepository.findByUserId(user.getId()).stream()
                .filter(t -> ACTIVE_STATUSES.contains(t.getStatus()))
                .count();

        int max = props.getLoan().getMaxActiveBooks();
        if (active >= max) {
            throw new BusinessRuleException(
                    user.getName() + " already has " + active + " active items "
                            + "(the limit is " + max + "). Return something first.",
                    "BORROWING_LIMIT_REACHED");
        }
    }

    /** Feeds {@code User.previouslyReadGenre}, which WS-04 uses for recommendations. */
    private void recordGenreForRecommendations(String userObjectId, String genre) {
        if (genre == null || genre.isBlank()) return;

        userRepository.findById(userObjectId).ifPresent(user -> {
            List<String> genres = user.getPreviouslyReadGenre() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(user.getPreviouslyReadGenre());

            if (!genres.contains(genre)) {
                genres.add(genre);
                user.setPreviouslyReadGenre(genres);
                userRepository.save(user);
            }
        });
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<Transaction> getActiveTransactionsForUser(String memberCode) {
        User user = userRepository.findByUserId(memberCode)
                .orElseThrow(() -> new ResourceNotFoundException("User", memberCode));

        return transactionRepository.findByUserId(user.getId()).stream()
                .filter(t -> ACTIVE_STATUSES.contains(t.getStatus()))
                .toList();
    }

    /** Returned loans, newest first — feeds the WS-04 reading-history panel. */
    public List<Transaction> getHistoryForUser(String memberCode, int limit) {
        User user = userRepository.findByUserId(memberCode)
                .orElseThrow(() -> new ResourceNotFoundException("User", memberCode));

        return transactionRepository.findByUserId(user.getId()).stream()
                .filter(t -> "RETURNED".equals(t.getStatus()))
                .sorted(Comparator.comparing(
                        Transaction::getReturnDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    public List<Transaction> getAllActiveTransactions() {
        return transactionRepository.findByStatusIn(List.copyOf(ACTIVE_STATUSES));
    }
}
