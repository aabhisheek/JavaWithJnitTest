package com.zest.productapi.dto.response;

import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String username;
    private Set<String> roles;

    public static AuthResponse of(String accessToken, String refreshToken,
                                  String username, Set<String> roles) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .username(username)
                .roles(roles)
                .build();
    }
}
