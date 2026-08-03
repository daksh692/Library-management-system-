package com.library.lms.service;

import com.library.lms.dto.ReturnRequest;
import com.library.lms.dto.TransactionRequest;
import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Core service managing the lifecycle of library transactions.
 * Handles issuing books, managing waitlist queues when stock is depleted,
 * and processing returns along with late fees and condition penalties.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * Attempts to issue a book to a user.
     * If available copies > 0, the book is directly issued.
     * If available copies == 0, the user is placed into a sequential BOOKED_IN_QUEUE.
     *
     * @param request The transaction request containing Book ID and User ID.
     * @return The created Transaction entity.
     */
    public Transaction issueBook(TransactionRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseGet(() -> {
                    List<Book> books = bookRepository.findByIsbn(request.getBookId());
                    if (books != null && !books.isEmpty()) {
                        return books.get(0);
                    }
                    throw new RuntimeException("Book not found");
                });
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Transaction transaction = Transaction.builder()
                .bookId(book.getId())
                .userId(user.getId())
                .build();

        if (book.getAvailableCopies() > 0) {
            book.setAvailableCopies(book.getAvailableCopies() - 1);
            bookRepository.save(book);

            transaction.setStatus("ISSUED");
            transaction.setIssueDate(new Date());
            
            // 14 days due date
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 14);
            transaction.setDueDate(cal.getTime());
        } else {
            transaction.setStatus("BOOKED_IN_QUEUE");
            // Determine queue sequence
            long queueCount = transactionRepository.findByBookId(book.getId()).stream()
                    .filter(t -> "BOOKED_IN_QUEUE".equals(t.getStatus()))
                    .count();
            transaction.setQueueSequence((int) queueCount + 1);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * Processes the return of an active or held book.
     * Calculates any applicable late fees ($1/day) and physical condition penalties.
     * Automatically triggers the next person in the waitlist if applicable.
     *
     * @param request The return request specifying transaction ID and book condition.
     * @return The updated Transaction entity marked as RETURNED.
     */
    public Transaction returnBook(ReturnRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"ISSUED".equals(transaction.getStatus()) && !"HELD_FOR_PICKUP".equals(transaction.getStatus())) {
            throw new RuntimeException("Transaction is not currently active or issued");
        }

        Book book = bookRepository.findById(transaction.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found"));

        transaction.setStatus("RETURNED");
        transaction.setReturnDate(new Date());
        transaction.setBookConditionOnReturn(request.getCondition());

        double penalty = 0.0;

        // Late fee calculation ($1 per day)
        if (transaction.getDueDate() != null && transaction.getReturnDate().after(transaction.getDueDate())) {
            long diffInMillies = Math.abs(transaction.getReturnDate().getTime() - transaction.getDueDate().getTime());
            long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            penalty += diff * 1.0; 
        }

        // Condition penalty
        if ("DAMAGED".equals(request.getCondition()) || "LOST".equals(request.getCondition())) {
            penalty += (book.getPrice() != null ? book.getPrice() : 50.0);
        }
        
        transaction.setPenaltyApplied(penalty);
        transactionRepository.save(transaction);

        // Process Waitlist Queue
        if (!"LOST".equals(request.getCondition())) {
            processNextInQueue(book);
        } else {
            // Book is lost, decrement total copies
            book.setTotalCopies(Math.max(0, book.getTotalCopies() - 1));
            bookRepository.save(book);
        }

        return transaction;
    }

    /**
     * Advances the queue for a specific book.
     * Marks the next person's status as HELD_FOR_PICKUP and begins their 48-hour window.
     * If the queue is empty, the book is simply restocked into available copies.
     *
     * @param book The book whose waitlist needs processing.
     */
    public void processNextInQueue(Book book) {
        List<Transaction> queue = transactionRepository.findByBookId(book.getId()).stream()
                .filter(t -> "BOOKED_IN_QUEUE".equals(t.getStatus()))
                .sorted((t1, t2) -> t1.getQueueSequence().compareTo(t2.getQueueSequence()))
                .collect(Collectors.toList());

        if (!queue.isEmpty()) {
            Transaction nextInQueue = queue.get(0);
            nextInQueue.setStatus("HELD_FOR_PICKUP");
            nextInQueue.setIssueDate(new Date()); // Start 48 hour window
            transactionRepository.save(nextInQueue);
        } else {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookRepository.save(book);
        }
    }

    public List<Transaction> getActiveTransactionsForUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        return transactionRepository.findByUserId(user.getId()).stream()
                .filter(t -> "ISSUED".equals(t.getStatus()) || "HELD_FOR_PICKUP".equals(t.getStatus()) || "BOOKED_IN_QUEUE".equals(t.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Transaction> getAllActiveTransactions() {
        return transactionRepository.findAll().stream()
                .filter(t -> "ISSUED".equals(t.getStatus()) || "HELD_FOR_PICKUP".equals(t.getStatus()) || "BOOKED_IN_QUEUE".equals(t.getStatus()))
                .collect(Collectors.toList());
    }
}
