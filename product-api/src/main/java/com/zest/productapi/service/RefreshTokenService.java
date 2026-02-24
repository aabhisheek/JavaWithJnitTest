package com.zest.productapi.service;

import com.zest.productapi.entity.RefreshToken;
import com.zest.productapi.entity.User;
import com.zest.productapi.exception.TokenRefreshException;
import com.zest.productapi.repository.RefreshTokenRepository;
import com.zest.productapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Manages refresh token lifecycle with server-side storage.
 *
 * <p><b>Security:</b> Only the SHA-256 hash of the raw UUID is persisted.
 * Even if the {@code refresh_tokens} table is compromised, attackers cannot
 * replay the tokens because they don't have the raw values.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository         userRepository;

    /**
     * Creates a new refresh token for the given user (old token is deleted first –
     * rotation).
     *
     * @return the <em>raw</em> UUID token that must be sent to the client.
     *         The DB only stores its SHA-256 hash.
     */
    @Transactional
    public String createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Delete previous token (rotation – one active token per user)
        refreshTokenRepository.deleteByUser(user);

        String rawToken    = UUID.randomUUID().toString();
        String hashedToken = DigestUtils.sha256Hex(rawToken);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(hashedToken)                   // store hash, never raw
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        refreshTokenRepository.save(token);
        return rawToken;                              // return raw to caller / client
    }

    /**
     * Looks up a refresh token by hashing the raw client-supplied value first.
     */
    @Transactional
    public RefreshToken findByToken(String rawToken) {
        String hashed = DigestUtils.sha256Hex(rawToken);
        return refreshTokenRepository.findByToken(hashed)
                .orElseThrow(() -> new TokenRefreshException(rawToken, "Refresh token not found"));
    }

    /** Throws {@link TokenRefreshException} if the token is expired; deletes it too. */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("[redacted]",
                    "Refresh token has expired – please log in again");
        }
        return token;
    }

    @Transactional
    public void deleteByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        refreshTokenRepository.deleteByUser(user);
    }
}
