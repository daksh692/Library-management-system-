package com.library.lms.controller;

import com.library.lms.dto.AuthResponse;
import com.library.lms.dto.LoginRequest;
import com.library.lms.dto.RegisterRequest;
import com.library.lms.model.User;
import com.library.lms.repository.UserRepository;
import com.library.lms.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@Tag(name = "Authentication", description = "Login and registration")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final com.library.lms.service.LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        long wait = loginAttemptService.secondsUntilAllowed(request.getUserId());
        if (wait > 0) {
            throw new com.library.lms.exception.BusinessRuleException(
                    "Too many failed attempts. Please wait " + wait + " seconds before trying again.",
                    "LOGIN_BACKOFF");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
            );

            loginAttemptService.recordSuccess(request.getUserId());

            User user = (User) authentication.getPrincipal();
            String token = jwtUtil.generateToken(user);

            ResponseCookie cookie = ResponseCookie.from("token", token)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(24 * 60 * 60)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(AuthResponse.builder()
                        .userId(user.getUserId())
                        .name(user.getName())
                        .role(user.getRole())
                        .build());

        } catch (org.springframework.security.core.AuthenticationException ex) {
            loginAttemptService.recordFailure(request.getUserId());
            throw ex;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return ResponseEntity.badRequest().body("Phone number already registered.");
        }

        // Generate unique userId like LIB-2026-XXXX
        String generatedUserId = "LIB-2026-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        User user = User.builder()
                .userId(generatedUserId)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .isDeleted(false)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully. Your User ID is: " + generatedUserId);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).build();
        }
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(AuthResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .role(user.getRole())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out successfully");
    }
}
