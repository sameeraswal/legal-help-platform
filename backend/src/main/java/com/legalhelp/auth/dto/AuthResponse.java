package com.legalhelp.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, UserProfileResponse user) {
}
