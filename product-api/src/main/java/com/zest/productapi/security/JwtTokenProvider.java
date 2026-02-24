package com.zest.productapi.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        return buildToken(principal.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        return buildToken(username);
    }

    private String buildToken(String subject) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(subject)
                .id(UUID.randomUUID().toString())   // jti – unique per token, used for blacklisting
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return claims(token).getSubject();
    }

    /** Returns the JWT ID claim (used as the blacklist key). */
    public String getJtiFromToken(String token) {
        return claims(token).getId();
    }

    /**
     * Returns how many milliseconds remain until the token expires.
     * Returns 0 if the token is already past its expiry.
     */
    public long getRemainingValidityMillis(String token) {
        long expiry = claims(token).getExpiration().getTime();
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
