package com.library.lms.service;

import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.TransactionRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only browsing intelligence: new arrivals, personalised recommendations,
 * related titles, and availability forecasting.
 *
 * <p>Deliberately separate from {@link BookService}, which owns catalogue writes.
 * Nothing here mutates state.</p>
 */
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final BookRepository bookRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /** Most recently catalogued titles. The PRD's 4x2 grid wants 8. */
    public List<Book> newArrivals(int limit) {
        return bookRepository.findByIsDeletedFalseOrderByCreatedAtDesc(
                PageRequest.of(0, limit));
    }

    /**
     * Books in genres this patron has borrowed before, excluding anything they
     * currently hold or have already read.
     *
     * <p>Falls back to new arrivals for a patron with no history, so the carousel
     * is never empty — an empty "Curated For You" is worse than a generic one.</p>
     *
     * @param memberCode the LIB-YYYY-NNNN member id
     * @param limit      how many to return; the PRD asks for 5
     */
    public List<Book> recommendationsFor(String memberCode, int limit) {
        User user = userRepository.findByUserId(memberCode)
                .orElseThrow(() -> new ResourceNotFoundException("User", memberCode));

        List<String> genres = user.getPreviouslyReadGenre();
        if (genres == null || genres.isEmpty()) {
            return newArrivals(limit);
        }

        // Everything this patron has ever transacted on — don't recommend it back.
        Set<String> seenBookIds = transactionRepository.findByUserId(user.getId())
                .stream()
                .map(Transaction::getBookId)
                .collect(java.util.stream.Collectors.toSet());

        List<Book> candidates = bookRepository
                .findByIsDeletedFalseAndGenreIn(genres)
                .stream()
                .filter(b -> !seenBookIds.contains(b.getId()))
                .sorted(Comparator
                        // available first, then newest
                        .comparing((Book b) -> b.getAvailableCopies() != null && b.getAvailableCopies() > 0)
                        .reversed()
                        .thenComparing(Book::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();

        if (candidates.size() < limit) {
            // Top up from new arrivals so the row always fills.
            List<Book> topped = new ArrayList<>(candidates);
            Set<String> chosen = new LinkedHashSet<>(candidates.stream().map(Book::getId).toList());

            for (Book b : newArrivals(limit * 2)) {
                if (topped.size() >= limit) break;
                if (chosen.add(b.getId()) && !seenBookIds.contains(b.getId())) {
                    topped.add(b);
                }
            }
            return topped;
        }

        return candidates;
    }

    /**
     * Titles sharing a genre or an author with the given book.
     * Same-author matches rank above same-genre ones.
     */
    public List<Book> relatedTo(String bookId, int limit) {
        Book source = bookRepository.findById(bookId)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        return bookRepository.findByIsDeletedFalse().stream()
                .filter(b -> !b.getId().equals(source.getId()))
                .filter(b -> matches(b.getGenre(), source.getGenre())
                        || matches(b.getAuthor(), source.getAuthor()))
                .sorted(Comparator
                        .comparing((Book b) -> matches(b.getAuthor(), source.getAuthor()))
                        .reversed())
                .limit(limit)
                .toList();
    }

    /**
     * When the next copy is expected back.
     *
     * @return the earliest due date among currently issued copies, or {@code null}
     *         if a copy is already free or every copy is unaccounted for
     */
    public Date estimatedAvailability(String bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) return null;
        if (book.getAvailableCopies() != null && book.getAvailableCopies() > 0) return null;

        return transactionRepository
                .findByBookIdAndStatusOrderByDueDateAsc(bookId, "ISSUED")
                .stream()
                .map(Transaction::getDueDate)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }
}
