package com.library.lms.controller;

import com.library.lms.dto.BookDto;
import com.library.lms.dto.UserDto;
import com.library.lms.dto.response.BookResponse;
import com.library.lms.dto.response.UserResponse;
import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.PaymentRepository;
import com.library.lms.repository.TransactionRepository;
import com.library.lms.service.BookService;
import com.library.lms.service.TransactionService;
import com.library.lms.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Admin / Librarians", description = "Catalogue and directory management")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookService bookService;
    private final UserService userService;
    private final TransactionService transactionService;
    private final BookRepository bookRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    // --- Book Endpoints ---
    
    @PostMapping("/books")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookDto dto) {
        return ResponseEntity.ok(BookResponse.from(bookService.addBook(dto)));
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable String id, @Valid @RequestBody BookDto dto) {
        return ResponseEntity.ok(BookResponse.from(bookService.updateBook(id, dto)));
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable String id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok().build();
    }

    // --- User Endpoints ---

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getActiveUsers() {
        return ResponseEntity.ok(userService.getActiveUsers().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList()));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserDto dto) {
        return ResponseEntity.ok(UserResponse.from(userService.addUser(dto)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String id, @Valid @RequestBody UserDto dto) {
        return ResponseEntity.ok(UserResponse.from(userService.updateUser(id, dto)));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Directory search. Matches phone, member id, name, or email — phone first,
     * because it is the PRD's primary lookup key at the desk.
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(
                userService.search(query).stream().map(UserResponse::from).toList());
    }

    @PostMapping("/users/{id}/renew-card")
    public ResponseEntity<UserResponse> renewCard(@PathVariable String id) {
        return ResponseEntity.ok(UserResponse.from(userService.renewCard(id)));
    }

    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable String userId) {
        return ResponseEntity.ok(transactionService.getActiveTransactionsForUser(userId));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Long>> getMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("totalBooks", bookRepository.countByIsDeletedFalse());
        metrics.put("activeLoans", transactionRepository.countByStatus("ISSUED"));
        metrics.put("overdueItems", transactionRepository.countByStatusAndDueDateBefore("ISSUED", new Date()));
        metrics.put("unpaidFines", paymentRepository.countByStatus("PENDING"));
        return ResponseEntity.ok(metrics);
    }
}
