package com.legalhelp.auth.service;

import com.legalhelp.auth.dto.*;
import com.legalhelp.auth.entity.RefreshToken;
import com.legalhelp.auth.entity.User;
import com.legalhelp.auth.entity.UserStatus;
import com.legalhelp.auth.repository.RefreshTokenRepository;
import com.legalhelp.auth.repository.UserRepository;
import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.exception.ConflictException;
import com.legalhelp.common.security.JwtService;
import com.legalhelp.common.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot self-register");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User(request.role(), request.name(), request.email(), request.phone(),
                passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BadCredentialsException("This account has been suspended");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = hash(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }
        stored.revoke();
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(hash(refreshToken)).ifPresent(RefreshToken::revoke);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = generateOpaqueToken();
        refreshTokenRepository.save(new RefreshToken(user.getId(), hash(rawRefreshToken), Instant.now().plus(refreshTokenTtl)));
        UserProfileResponse profile = new UserProfileResponse(user.getId(), user.getRole(), user.getName(), user.getEmail(), user.getPhone());
        return new AuthResponse(accessToken, rawRefreshToken, profile);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
