package com.library.lms.service;

import com.library.lms.dto.BookDto;
import com.library.lms.exception.BusinessRuleException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Book;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Paginated, soft-delete-aware catalogue listing. */
    public Page<Book> getActiveBooks(Pageable pageable) {
        return bookRepository.findByIsDeletedFalse(pageable);
    }

    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    /**
     * Text-index search, falling back to regex when the index is absent
     * (a fresh database, or a local dev instance where it was never created).
     */
    public List<Book> searchBooks(String query) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.trim();

        try {
            org.springframework.data.mongodb.core.query.TextCriteria criteria = org.springframework.data.mongodb.core.query.TextCriteria.forDefaultLanguage().matching(q);
            org.springframework.data.mongodb.core.query.Query textQuery = org.springframework.data.mongodb.core.query.TextQuery.queryText(criteria)
                    .sortByScore()
                    .addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("isDeleted").is(false));

            List<Book> results = mongoTemplate.find(textQuery, Book.class);

            // ISBN is not in the text index — match it separately.
            if (results.isEmpty()) {
                return bookRepository.search(q);
            }
            return results;
        } catch (org.springframework.data.mongodb.UncategorizedMongoDbException e) {
            // Text index might not be ready/exist, fallback to regex search
            return bookRepository.search(q);
        }
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
