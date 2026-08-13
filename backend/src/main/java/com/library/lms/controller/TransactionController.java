package com.library.lms.controller;

import com.library.lms.config.LibraryProperties;
import com.library.lms.dto.ReturnRequest;
import com.library.lms.dto.TransactionRequest;
import com.library.lms.dto.response.TransactionResponse;
import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.UserRepository;
import com.library.lms.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "Transactions", description = "Issuing, returning, reservations, and fines")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LibraryProperties props;

    private TransactionResponse enrich(Transaction txn) {
        Book book = bookRepository.findById(txn.getBookId()).orElse(null);
        User user = userRepository.findById(txn.getUserId()).orElse(null);
        return TransactionResponse.of(txn, book, user, props.getHold().getWindowHours());
    }

    // Admin Endpoints
    @Operation(
        summary = "Issue a book or enqueue the patron",
        description = """
            Issues immediately when a copy is free. When none is, the patron joins the
            reservation queue and is notified as soon as one is returned.

            Rejected when the card has expired, the borrowing limit is reached, or the
            patron already holds this title.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issued, or queued"),
        @ApiResponse(responseCode = "404", description = "Book or patron not found"),
        @ApiResponse(responseCode = "409", description = "A borrowing rule was violated")
    })
    @PostMapping("/admin/transactions/issue")
    public ResponseEntity<TransactionResponse> issueBook(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(enrich(transactionService.issueBook(request)));
    }

    @PostMapping("/admin/transactions/return")
    public ResponseEntity<TransactionResponse> returnBook(@Valid @RequestBody ReturnRequest request) {
        return ResponseEntity.ok(enrich(transactionService.returnBook(request)));
    }
    
    @GetMapping("/admin/transactions/active")
    public ResponseEntity<List<TransactionResponse>> getAllActiveTransactions() {
        return ResponseEntity.ok(
                transactionService.getAllActiveTransactions().stream().map(this::enrich).toList());
    }

    /** Patron collected a book that was held for them. */
    @PostMapping("/admin/transactions/{id}/handover")
    public ResponseEntity<TransactionResponse> handover(@PathVariable String id) {
        return ResponseEntity.ok(enrich(transactionService.handoverHeldBook(id)));
    }

    /** Fine settled at the desk. */
    @PostMapping("/admin/transactions/{id}/settle-penalty")
    public ResponseEntity<TransactionResponse> settlePenalty(@PathVariable String id) {
        return ResponseEntity.ok(enrich(transactionService.settlePenalty(id)));
    }

    // User Endpoints
    @GetMapping("/user/transactions/active")
    public ResponseEntity<List<TransactionResponse>> getUserActiveTransactions(Principal principal) {
        // principal.getName() returns the userId (e.g., LIB-2026-XXXX) from JWT Token
        String userId = principal.getName();
        return ResponseEntity.ok(
                transactionService.getActiveTransactionsForUser(userId).stream().map(this::enrich).toList());
    }

    /** Reading history for the signed-in patron. */
    @GetMapping("/user/transactions/history")
    public ResponseEntity<List<TransactionResponse>> history(
            Principal principal,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(
                transactionService.getHistoryForUser(principal.getName(), limit)
                        .stream().map(this::enrich).toList());
    }
}
