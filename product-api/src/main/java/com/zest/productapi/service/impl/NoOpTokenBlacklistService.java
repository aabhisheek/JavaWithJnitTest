package com.zest.productapi.service.impl;

import com.zest.productapi.service.TokenBlacklistService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-operation implementation of {@link TokenBlacklistService}.
 *
 * <p>Active when {@code app.token-blacklist.enabled=false}.
 * Used in tests to avoid a Redis dependency – tokens are never blacklisted.
 */
@Service
@ConditionalOnProperty(name = "app.token-blacklist.enabled", havingValue = "false")
public class NoOpTokenBlacklistService implements TokenBlacklistService {

    @Override
    public void blacklist(String jti, long ttlMillis) {
        // intentional no-op
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return false;
    }
}
