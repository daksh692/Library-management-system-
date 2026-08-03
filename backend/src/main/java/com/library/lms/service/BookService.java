package com.library.lms.service;

import com.library.lms.dto.BookDto;
import com.library.lms.model.Book;
import com.library.lms.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling inventory management for books.
 * Responsible for adding, updating, retrieving, and soft-deleting books.
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

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
                .orElseThrow(() -> new RuntimeException("Book not found"));
        if (book.isDeleted()) {
            throw new RuntimeException("Book not found");
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
     * Updates an existing book's metadata and handles copy count adjustments.
     * 
     * @param id The ID of the book to update.
     * @param dto The updated data.
     * @return The updated Book entity.
     */
    public Book updateBook(String id, BookDto dto) {
        Book book = getBookById(id);
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
        
        // Adjust available copies if total copies changed
        int diff = dto.getTotalCopies() - book.getTotalCopies();
        book.setTotalCopies(dto.getTotalCopies());
        book.setAvailableCopies(book.getAvailableCopies() + diff);
        
        return bookRepository.save(book);
    }

    /**
     * Soft-deletes a book from the catalog to preserve transaction history.
     *
     * @param id The ID of the book to soft delete.
     */
    public void deleteBook(String id) {
        Book book = getBookById(id);
        book.setDeleted(true);
        bookRepository.save(book);
    }
}
