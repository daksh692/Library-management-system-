package com.library.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransactionRequest {
    @NotBlank(message = "Book ID is required")
    private String bookId;

    @NotBlank(message = "User ID is required")
    private String userId;
}
