package com.library.lms.service;

import com.library.lms.dto.ReturnRequest;
import com.library.lms.dto.TransactionRequest;
import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import com.library.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIssueBook_CopiesAvailable() {
        TransactionRequest req = new TransactionRequest();
        req.setBookId("book1");
        req.setUserId("user1");

        Book book = Book.builder().id("book1").availableCopies(1).build();
        User user = User.builder().id("u1").userId("user1").build();

        when(bookRepository.findById("book1")).thenReturn(Optional.of(book));
        when(userRepository.findByUserId("user1")).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        Transaction result = transactionService.issueBook(req);

        assertEquals("ISSUED", result.getStatus());
        assertEquals(0, book.getAvailableCopies());
        assertNotNull(result.getDueDate());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void testIssueBook_NoCopies_Waitlist() {
        TransactionRequest req = new TransactionRequest();
        req.setBookId("book1");
        req.setUserId("user1");

        Book book = Book.builder().id("book1").availableCopies(0).build();
        User user = User.builder().id("u1").userId("user1").build();

        when(bookRepository.findById("book1")).thenReturn(Optional.of(book));
        when(userRepository.findByUserId("user1")).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        Transaction result = transactionService.issueBook(req);

        assertEquals("BOOKED_IN_QUEUE", result.getStatus());
        assertEquals(0, book.getAvailableCopies());
    }

    @Test
    void testReturnBook_LostCondition() {
        ReturnRequest req = new ReturnRequest();
        req.setTransactionId("txn1");
        req.setCondition("LOST");

        Transaction txn = Transaction.builder().id("txn1").bookId("book1").status("ISSUED").dueDate(new Date()).build();
        Book book = Book.builder().id("book1").totalCopies(1).price(50.0).build();

        when(transactionRepository.findById("txn1")).thenReturn(Optional.of(txn));
        when(bookRepository.findById("book1")).thenReturn(Optional.of(book));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(txn);

        Transaction result = transactionService.returnBook(req);

        assertEquals("RETURNED", result.getStatus());
        assertEquals(50.0, result.getPenaltyApplied());
        assertEquals(0, book.getTotalCopies()); // Lost decrements total copies
    }
}
