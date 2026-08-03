package com.library.lms.service;

import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service responsible for managing automated background tasks related to 
 * transaction queues and waitlists.
 */
@Service
@RequiredArgsConstructor
public class QueueMgmtService {

    private final TransactionRepository transactionRepository;
    private final BookRepository bookRepository;
    private final TransactionService transactionService;

    /**
     * Scheduled background job that runs every hour (3600000 ms).
     * Sweeps the transactions for any held books that have exceeded the 48-hour pickup window.
     * If a hold has expired, it cancels the hold and automatically issues the book to the 
     * next user in the queue or returns it to the available stock pool.
     */
    @Scheduled(fixedRate = 3600000)
    public void sweepExpiredHolds() {
        System.out.println("Running scheduled sweep for expired holds...");
        
        List<Transaction> holds = transactionRepository.findByStatus("HELD_FOR_PICKUP");
        Date now = new Date();

        for (Transaction hold : holds) {
            if (hold.getIssueDate() != null) {
                long diffInMillies = Math.abs(now.getTime() - hold.getIssueDate().getTime());
                long hours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                
                if (hours >= 48) {
                    System.out.println("Hold expired for transaction: " + hold.getId());
                    hold.setStatus("CANCELLED_HOLD");
                    transactionRepository.save(hold);
                    
                    Book book = bookRepository.findById(hold.getBookId()).orElse(null);
                    if (book != null) {
                        transactionService.processNextInQueue(book);
                    }
                }
            }
        }
    }
}
