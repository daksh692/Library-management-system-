package com.library.lms.repository;

import com.library.lms.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByUserId(String userId);

    List<Transaction> findByBookId(String bookId);

    List<Transaction> findByStatus(String status);

    long countByStatus(String status);
    long countByStatusAndDueDateBefore(String status, java.util.Date date);

    // --- added in WS-02 ---
    List<Transaction> findByBookIdAndStatus(String bookId, String status);

    List<Transaction> findByUserIdAndBookId(String userId, String bookId);

    List<Transaction> findByStatusIn(List<String> statuses);

    /** Used by WS-04 §5 to estimate when the next copy frees up. */
    List<Transaction> findByBookIdAndStatusOrderByDueDateAsc(String bookId, String status);
}
