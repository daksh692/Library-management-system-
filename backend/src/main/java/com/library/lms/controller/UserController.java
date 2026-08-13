package com.library.lms.controller;

import com.library.lms.config.LibraryProperties;
import com.library.lms.dto.ReserveRequest;
import com.library.lms.dto.TransactionRequest;
import com.library.lms.dto.response.BookResponse;
import com.library.lms.dto.response.TransactionResponse;
import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.UserRepository;
import com.library.lms.service.DiscoveryService;
import com.library.lms.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Patron-facing endpoints. Everything here is scoped to the caller's own JWT
 * subject — a patron can never address another patron's data.
 */
@Tag(name = "User / Patron", description = "Patron-facing operations scoped to the authenticated user")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final TransactionService transactionService;
    private final DiscoveryService discoveryService;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LibraryProperties props;

    /**
     * Everything the home screen needs, in one round trip.
     *
     * @return keys: {@code activeLoans}, {@code history}, {@code recommendations},
     *         {@code newArrivals}, {@code outstandingFines}
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Principal principal) {
        String memberCode = principal.getName();

        List<TransactionResponse> active = transactionService
                .getActiveTransactionsForUser(memberCode).stream()
                .map(this::enrich).toList();

        List<TransactionResponse> history = transactionService
                .getHistoryForUser(memberCode, 4).stream()
                .map(this::enrich).toList();

        List<BookResponse> recommendations = discoveryService
                .recommendationsFor(memberCode, 5).stream()
                .map(BookResponse::from).toList();

        List<BookResponse> newArrivals = discoveryService
                .newArrivals(8).stream()
                .map(BookResponse::from).toList();

        double fines = userRepository.findByUserId(memberCode)
                .map(u -> transactionService.outstandingFines(u.getId()))
                .orElse(0.0);

        return ResponseEntity.ok(Map.of(
                "activeLoans",      active,
                "history",          history,
                "recommendations",  recommendations,
                "newArrivals",      newArrivals,
                "outstandingFines", fines
        ));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<BookResponse>> recommendations(
            Principal principal,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                discoveryService.recommendationsFor(principal.getName(), limit)
                        .stream().map(BookResponse::from).toList());
    }

    /** Self-service reservation. The patron can only ever queue themselves. */
    @PostMapping("/reservations")
    public ResponseEntity<TransactionResponse> reserve(
            Principal principal,
            @Valid @RequestBody ReserveRequest request) {

        TransactionRequest txnRequest = new TransactionRequest();
        txnRequest.setBookId(request.getBookId());
        txnRequest.setUserId(principal.getName());   // never trust a body-supplied user id

        return ResponseEntity.ok(enrich(transactionService.issueBook(txnRequest)));
    }

    private TransactionResponse enrich(Transaction txn) {
        Book book = bookRepository.findById(txn.getBookId()).orElse(null);
        User user = userRepository.findById(txn.getUserId()).orElse(null);
        return TransactionResponse.of(txn, book, user, props.getHold().getWindowHours());
    }
}
