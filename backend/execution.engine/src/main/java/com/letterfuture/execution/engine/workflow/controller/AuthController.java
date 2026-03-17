package com.letterfuture.execution.engine.workflow.controller;


import com.letterfuture.execution.engine.auth.AuthService;
import com.letterfuture.execution.engine.workflow.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@CookieValue("refreshToken") String refreshToken) {
        return authService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@CookieValue("refreshToken") String refreshToken) {
        LogoutResponse response = authService.logout(refreshToken);

        // Clear the refresh token cookie
        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .path("/")
                .httpOnly(true)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(response);
    }

    @GetMapping("/me")
    public MeResponse me(@RequestHeader("Authorization") String authHeader) {
        // Extract token from "Bearer <token>"
        if (!authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        String token = authHeader.substring(7);
        return authService.me(token);
    }
}
