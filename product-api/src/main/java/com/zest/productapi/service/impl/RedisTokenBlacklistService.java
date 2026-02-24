package com.zest.productapi.service.impl;

import com.zest.productapi.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-backed implementation of {@link TokenBlacklistService}.
 *
 * <p>Each blacklisted JTI is stored as a Redis key with a TTL equal to the
 * token's remaining validity. Keys expire automatically – no cleanup job needed.
 *
 * <p>Active when {@code app.token-blacklist.enabled=true} (the production default).
 */
@Service
@ConditionalOnProperty(name = "app.token-blacklist.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String jti, long ttlMillis) {
        if (ttlMillis <= 0) return;           // already expired – nothing to store
        String key = KEY_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "1", ttlMillis, TimeUnit.MILLISECONDS);
        log.debug("Blacklisted token jti={} ttl={}ms", jti, ttlMillis);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
