package com.library.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReturnRequest {
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotBlank(message = "Condition is required")
    @Pattern(regexp = "^(GOOD|DAMAGED|LOST)$", message = "Condition must be GOOD, DAMAGED, or LOST")
    private String condition;
}
