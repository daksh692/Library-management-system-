package com.library.lms.controller;

import com.library.lms.dto.ReturnRequest;
import com.library.lms.dto.TransactionRequest;
import com.library.lms.model.Transaction;
import com.library.lms.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // Admin Endpoints
    @PostMapping("/admin/transactions/issue")
    public ResponseEntity<Transaction> issueBook(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.issueBook(request));
    }

    @PostMapping("/admin/transactions/return")
    public ResponseEntity<Transaction> returnBook(@Valid @RequestBody ReturnRequest request) {
        return ResponseEntity.ok(transactionService.returnBook(request));
    }
    
    @GetMapping("/admin/transactions/active")
    public ResponseEntity<List<Transaction>> getAllActiveTransactions() {
        return ResponseEntity.ok(transactionService.getAllActiveTransactions());
    }

    // User Endpoints
    @GetMapping("/user/transactions/active")
    public ResponseEntity<List<Transaction>> getUserActiveTransactions(Principal principal) {
        // principal.getName() returns the userId (e.g., LIB-2026-XXXX) from JWT Token
        String userId = principal.getName();
        return ResponseEntity.ok(transactionService.getActiveTransactionsForUser(userId));
    }
}
