package com.zest.productapi.service.impl;

import com.zest.productapi.dto.request.LoginRequest;
import com.zest.productapi.dto.request.RefreshTokenRequest;
import com.zest.productapi.dto.request.RegisterRequest;
import com.zest.productapi.dto.response.AuthResponse;
import com.zest.productapi.entity.RefreshToken;
import com.zest.productapi.entity.User;
import com.zest.productapi.repository.UserRepository;
import com.zest.productapi.security.JwtTokenProvider;
import com.zest.productapi.service.AuthService;
import com.zest.productapi.service.RefreshTokenService;
import com.zest.productapi.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtTokenProvider      jwtTokenProvider;
    private final RefreshTokenService   refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        Set<String> roles = (request.getRoles() == null || request.getRoles().isEmpty())
                ? Set.of("ROLE_USER")
                : request.getRoles();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .build();
        userRepository.save(user);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        String accessToken = jwtTokenProvider.generateAccessToken(auth);
        String rawRefresh  = refreshTokenService.createRefreshToken(request.getUsername());

        return AuthResponse.of(accessToken, rawRefresh, user.getUsername(), user.getRoles());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        String accessToken = jwtTokenProvider.generateAccessToken(auth);
        String rawRefresh  = refreshTokenService.createRefreshToken(request.getUsername());

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        return AuthResponse.of(accessToken, rawRefresh, user.getUsername(), user.getRoles());
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        String username    = refreshToken.getUser().getUsername();
        String accessToken = jwtTokenProvider.generateTokenFromUsername(username);

        // Rotation – old refresh token deleted, new one issued
        String newRawRefresh = refreshTokenService.createRefreshToken(username);

        User user = refreshToken.getUser();
        return AuthResponse.of(accessToken, newRawRefresh, user.getUsername(), user.getRoles());
    }

    @Override
    @Transactional
    public void logout(String username, String rawAccessToken) {
        // 1. Invalidate refresh token
        refreshTokenService.deleteByUsername(username);

        // 2. Blacklist the current access token so it can't be reused before its TTL
        if (StringUtils.hasText(rawAccessToken)
                && jwtTokenProvider.validateToken(rawAccessToken)) {

            String jti = jwtTokenProvider.getJtiFromToken(rawAccessToken);
            long   ttl = jwtTokenProvider.getRemainingValidityMillis(rawAccessToken);
            tokenBlacklistService.blacklist(jti, ttl);
            log.debug("Blacklisted access token on logout: jti={} ttl={}ms", jti, ttl);
        }

        SecurityContextHolder.clearContext();
    }
}
