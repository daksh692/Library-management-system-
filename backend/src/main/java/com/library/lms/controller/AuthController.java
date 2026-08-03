package com.library.lms.controller;

import com.library.lms.dto.AuthResponse;
import com.library.lms.dto.LoginRequest;
import com.library.lms.dto.RegisterRequest;
import com.library.lms.model.User;
import com.library.lms.repository.UserRepository;
import com.library.lms.util.JwtUtil;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .name(user.getName())
                .role(user.getRole())
                .build());
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
}
