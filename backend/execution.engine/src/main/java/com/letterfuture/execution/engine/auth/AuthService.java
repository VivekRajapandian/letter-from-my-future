package com.letterfuture.execution.engine.auth;


import com.letterfuture.execution.engine.enums.UserRole;
import com.letterfuture.execution.engine.workflow.domain.RefreshToken;
import com.letterfuture.execution.engine.workflow.domain.Users;
import com.letterfuture.execution.engine.workflow.dto.*;
import com.letterfuture.execution.engine.workflow.repository.UserRepository;
import com.letterfuture.execution.engine.workflow.repository.RefreshTokenRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);

        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                accessToken
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String usernameOrEmail = request.getUsernameOrEmail().trim().toLowerCase();
        String password = request.getPassword();

        Users user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or email"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is not active");
        }

        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                accessToken
        );
    }

    @Transactional(readOnly = true)
    public RefreshResponse refresh(String refreshToken) {
        // Validate the JWT refresh token
        UUID userId = jwtService.validateRefreshToken(refreshToken);

        // Check if the token exists in database and is not revoked
        String tokenHash = passwordEncoder.encode(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }

        // Get the user
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is not active");
        }

        // Generate new access token
        String accessToken = jwtService.generateAccessToken(user);

        return new RefreshResponse(accessToken);
    }

    @Transactional
    public LogoutResponse logout(String refreshToken) {
        // Hash the token to find it in DB
        String tokenHash = passwordEncoder.encode(refreshToken);

        // Find the token
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Revoke it
        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        return new LogoutResponse("Logged out successfully");
    }

    @Transactional(readOnly = true)
    public MeResponse me(String accessToken) {
        return jwtService.getUserInfoFromAccessToken(accessToken);
    }
}
