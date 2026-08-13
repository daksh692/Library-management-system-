package com.library.lms.controller;

import com.library.lms.config.LibraryProperties;
import com.library.lms.dto.response.BookResponse;
import com.library.lms.model.Book;
import com.library.lms.service.BookService;
import com.library.lms.service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Public Book Discovery", description = "Endpoints for searching and browsing the catalogue")
@RestController
@RequestMapping("/api/public/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final DiscoveryService discoveryService;
    private final LibraryProperties props;

    /** Paginated catalogue. Unbounded {@code findAll()} does not survive a real collection. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        Page<Book> result = bookService.getActiveBooks(
                PageRequest.of(page, Math.min(size, 100)));

        return ResponseEntity.ok(Map.of(
                "content",       result.getContent().stream().map(BookResponse::from).toList(),
                "page",          result.getNumber(),
                "size",          result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages",    result.getTotalPages(),
                "last",          result.isLast()
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(
                bookService.searchBooks(query).stream().map(BookResponse::from).toList());
    }

    /** Single book, with the estimated return date when nothing is on the shelf. */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable String id) {
        Book book = bookService.getBookById(id);
        return ResponseEntity.ok(
                BookResponse.from(book, discoveryService.estimatedAvailability(id)));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<List<BookResponse>> related(
            @PathVariable String id,
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(
                discoveryService.relatedTo(id, limit).stream().map(BookResponse::from).toList());
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<List<BookResponse>> newArrivals(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(
                discoveryService.newArrivals(limit).stream().map(BookResponse::from).toList());
    }

    @GetMapping("/policy")
    public ResponseEntity<Map<String, Object>> policy() {
        return ResponseEntity.ok(Map.of(
                "loanPeriodDays",  props.getLoan().getPeriodDays(),
                "maxActiveBooks",  props.getLoan().getMaxActiveBooks(),
                "holdWindowHours", props.getHold().getWindowHours(),
                "finePerDayLate",  props.getPenalty().getPerDayLate()
        ));
    }
}
