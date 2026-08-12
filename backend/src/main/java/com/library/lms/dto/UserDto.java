package com.library.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(ROLE_USER|ROLE_ADMIN)$",
             message = "Role must be either ROLE_USER or ROLE_ADMIN")
    private String role; // ROLE_USER or ROLE_ADMIN

    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password; // Optional when updating, required when creating
}
