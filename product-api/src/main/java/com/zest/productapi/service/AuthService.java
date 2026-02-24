package com.zest.productapi.service;

import com.zest.productapi.dto.request.LoginRequest;
import com.zest.productapi.dto.request.RefreshTokenRequest;
import com.zest.productapi.dto.request.RegisterRequest;
import com.zest.productapi.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Deletes the user's refresh token and blacklists the current access token
     * so it cannot be reused for the remainder of its TTL.
     *
     * @param username       authenticated user's username
     * @param rawAccessToken the raw Bearer token from the Authorization header
     *                       (may be {@code null} if the header was absent)
     */
    void logout(String username, String rawAccessToken);
}
