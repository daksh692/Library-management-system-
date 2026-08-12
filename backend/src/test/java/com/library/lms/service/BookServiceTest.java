package com.library.lms.service;

import com.library.lms.dto.BookDto;
import com.library.lms.model.Book;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BookService bookService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddBook_Success() {
        BookDto dto = new BookDto();
        dto.setName("Test Book");
        dto.setAuthor("Author");
        dto.setIsbn("12345");
        dto.setGenre("Fiction");
        dto.setLocation("A-1-1");
        dto.setTotalCopies(5);
        dto.setPrice(100.0);

        Book savedBook = Book.builder()
                .id("book1")
                .name("Test Book")
                .totalCopies(5)
                .availableCopies(5)
                .price(100.0)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        Book result = bookService.addBook(dto);

        assertNotNull(result);
        assertEquals(5, result.getTotalCopies());
        assertEquals(5, result.getAvailableCopies());
        assertEquals(100.0, result.getPrice());
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void testUpdateBook_AdjustsCopiesCorrectly() {
        Book existingBook = Book.builder()
                .id("book1")
                .totalCopies(5)
                .availableCopies(2)
                .build();

        BookDto dto = new BookDto();
        dto.setTotalCopies(7); // +2 copies

        when(bookRepository.findById("book1")).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenReturn(existingBook);

        Book result = bookService.updateBook("book1", dto);

        assertEquals(7, result.getTotalCopies());
        assertEquals(4, result.getAvailableCopies()); // 2 + 2
    }

    @Test
    void testDeleteBook_SoftDelete() {
        Book existingBook = Book.builder()
                .id("book1")
                .isDeleted(false)
                .build();

        when(bookRepository.findById("book1")).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenReturn(existingBook);

        bookService.deleteBook("book1");

        assertTrue(existingBook.isDeleted());
        verify(bookRepository, times(1)).save(existingBook);
    }
}
