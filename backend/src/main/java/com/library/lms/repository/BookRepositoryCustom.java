package com.library.lms.repository;

import com.library.lms.model.Book;

/** Atomic stock operations that Spring Data cannot express as derived queries. */
public interface BookRepositoryCustom {

    /**
     * Atomically decrements {@code availableCopies}, but only if it is currently > 0.
     *
     * @return the updated book, or {@code null} if no copy was available.
     */
    Book tryReserveCopy(String bookId);

    /** Atomically increments {@code availableCopies}. */
    Book releaseCopy(String bookId);

    /**
     * Atomically decrements {@code totalCopies} when a book is written off as lost.
     * Never drives the count below zero.
     */
    Book writeOffCopy(String bookId);
}
