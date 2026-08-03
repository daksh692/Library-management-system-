package com.library.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username/userId is required")
    private String userId;

    @NotBlank(message = "Password is required")
    private String password;
}
