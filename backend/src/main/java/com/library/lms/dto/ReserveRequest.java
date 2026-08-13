package com.library.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Self-service reservation body. The patron is taken from the JWT, never the payload. */
@Data
public class ReserveRequest {
    @NotBlank(message = "Book ID is required")
    private String bookId;
}
