package com.zest.productapi.service;

/**
 * Contract for blacklisting JWT access tokens after logout.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@code RedisTokenBlacklistService} – active in production (Redis-backed)</li>
 *   <li>{@code NoOpTokenBlacklistService} – active in tests (no external dependency)</li>
 * </ul>
 */
public interface TokenBlacklistService {

    /**
     * Marks a token as invalid for the remainder of its natural TTL.
     *
     * @param jti       the JWT ID claim of the access token
     * @param ttlMillis milliseconds until the token would have expired naturally
     */
    void blacklist(String jti, long ttlMillis);

    /**
     * @return {@code true} if the token has been explicitly invalidated via logout
     */
    boolean isBlacklisted(String jti);
}
