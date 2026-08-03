package com.library.lms.controller;

import com.library.lms.dto.BookDto;
import com.library.lms.dto.UserDto;
import com.library.lms.model.Book;
import com.library.lms.model.Transaction;
import com.library.lms.model.User;
import com.library.lms.service.BookService;
import com.library.lms.service.TransactionService;
import com.library.lms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookService bookService;
    private final UserService userService;
    private final TransactionService transactionService;

    // --- Book Endpoints ---
    
    @PostMapping("/books")
    public ResponseEntity<Book> createBook(@Valid @RequestBody BookDto dto) {
        return ResponseEntity.ok(bookService.addBook(dto));
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable String id, @Valid @RequestBody BookDto dto) {
        try {
            return ResponseEntity.ok(bookService.updateBook(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable String id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- User Endpoints ---

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserDto dto) {
        try {
            return ResponseEntity.ok(userService.addUser(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @Valid @RequestBody UserDto dto) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable String userId) {
        try {
            return ResponseEntity.ok(transactionService.getActiveTransactionsForUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
