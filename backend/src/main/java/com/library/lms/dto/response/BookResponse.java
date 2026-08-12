package com.library.lms.dto.response;

import com.library.lms.model.Book;

import java.util.Date;

/**
 * Outward-facing representation of a {@link Book}.
 *
 * <p>{@code estimatedAvailableOn} is populated only by endpoints that have the
 * transaction data to compute it (see WS-04 §5); elsewhere it is null.</p>
 */
public record BookResponse(
        String id,
        String isbn,
        String name,
        String author,
        String shortDescription,
        String longDescription,
        String genre,
        String photoUrl,
        String location,
        Integer totalCopies,
        Integer availableCopies,
        Double price,
        Date createdAt,
        Date estimatedAvailableOn
) {
    public static BookResponse from(Book book) {
        return from(book, null);
    }

    public static BookResponse from(Book book, Date estimatedAvailableOn) {
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getName(),
                book.getAuthor(),
                book.getShortDescription(),
                book.getLongDescription(),
                book.getGenre(),
                book.getPhotoUrl(),
                book.getLocation(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.getPrice(),
                book.getCreatedAt(),
                estimatedAvailableOn
        );
    }
}
