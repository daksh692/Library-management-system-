package com.library.lms.service;

import com.library.lms.dto.BookDto;
import com.library.lms.exception.BusinessRuleException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Book;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service handling inventory management for books.
 * Responsible for adding, updating, retrieving, and soft-deleting books.
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final TransactionRepository transactionRepository;

    public List<Book> getAllActiveBooks() {
        return bookRepository.findAll().stream()
                .filter(b -> !b.isDeleted())
                .collect(Collectors.toList());
    }

    /**
     * Search the catalog for books. Excludes soft-deleted entries.
     * Searches across name, author, genre, and ISBN fields.
     *
     * @param query The search string.
     * @return A list of matching books.
     */
    public List<Book> searchBooks(String query) {
        // Simplified search logic; in production use MongoTemplate text search
        return bookRepository.findAll().stream()
                .filter(b -> !b.isDeleted())
                .filter(b -> (b.getName() != null && b.getName().toLowerCase().contains(query.toLowerCase())) ||
                             (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(query.toLowerCase())) ||
                             (b.getGenre() != null && b.getGenre().toLowerCase().contains(query.toLowerCase())) ||
                             (b.getIsbn() != null && b.getIsbn().contains(query)))
                .collect(Collectors.toList());
    }

    public Book getBookById(String id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        if (book.isDeleted()) {
            throw new ResourceNotFoundException("Book", id);
        }
        return book;
    }

    /**
     * Creates a new book entry in the library catalog.
     * Initializes the available copies to equal total copies.
     *
     * @param dto The data transfer object containing book details.
     * @return The saved Book entity.
     */
    public Book addBook(BookDto dto) {
        Book book = Book.builder()
                .isbn(dto.getIsbn())
                .name(dto.getName())
                .author(dto.getAuthor())
                .shortDescription(dto.getShortDescription())
                .longDescription(dto.getLongDescription())
                .genre(dto.getGenre())
                .photoUrl(dto.getPhotoUrl())
                .location(dto.getLocation())
                .totalCopies(dto.getTotalCopies())
                .availableCopies(dto.getTotalCopies()) // Initially all are available
                .price(dto.getPrice() != null ? dto.getPrice() : 50.0)
                .isDeleted(false)
                .build();
        return bookRepository.save(book);
    }

    /**
     * Updates catalogue metadata and reconciles the copy count.
     *
     * @throws BusinessRuleException if the new total is below the number currently on loan
     */
    public Book updateBook(String id, BookDto dto) {
        Book book = getBookById(id);

        int currentTotal = book.getTotalCopies() == null ? 0 : book.getTotalCopies();
        int currentAvailable = book.getAvailableCopies() == null ? 0 : book.getAvailableCopies();
        int onLoan = currentTotal - currentAvailable;          // issued + held
        int newTotal = dto.getTotalCopies();

        if (newTotal < onLoan) {
            throw new BusinessRuleException(
                    "Cannot reduce to " + newTotal + " copies — " + onLoan
                            + " are currently issued or on hold. Reduce to " + onLoan + " or more.",
                    "COPIES_BELOW_ON_LOAN");
        }

        book.setIsbn(dto.getIsbn());
        book.setName(dto.getName());
        book.setAuthor(dto.getAuthor());
        book.setShortDescription(dto.getShortDescription());
        book.setLongDescription(dto.getLongDescription());
        book.setGenre(dto.getGenre());
        book.setPhotoUrl(dto.getPhotoUrl());
        book.setLocation(dto.getLocation());
        if (dto.getPrice() != null) {
            book.setPrice(dto.getPrice());
        }

        book.setTotalCopies(newTotal);
        book.setAvailableCopies(newTotal - onLoan);

        return bookRepository.save(book);
    }

    /**
     * Soft-deletes a book.
     *
     * @throws BusinessRuleException if any copy is still issued, held, or queued
     */
    public void deleteBook(String id) {
        Book book = getBookById(id);

        boolean hasActivity = transactionRepository
                .findByBookId(id).stream()
                .anyMatch(t -> Set.of("ISSUED", "HELD_FOR_PICKUP", "BOOKED_IN_QUEUE")
                        .contains(t.getStatus()));

        if (hasActivity) {
            throw new BusinessRuleException(
                    "'" + book.getName() + "' still has active loans or reservations "
                            + "and cannot be removed.",
                    "BOOK_HAS_ACTIVE_TRANSACTIONS");
        }

        book.setDeleted(true);
        bookRepository.save(book);
    }
}
