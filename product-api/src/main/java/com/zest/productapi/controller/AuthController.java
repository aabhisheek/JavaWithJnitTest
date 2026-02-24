package com.zest.productapi.controller;

import com.zest.productapi.dto.request.LoginRequest;
import com.zest.productapi.dto.request.RefreshTokenRequest;
import com.zest.productapi.dto.request.RegisterRequest;
import com.zest.productapi.dto.response.ApiResponse;
import com.zest.productapi.dto.response.AuthResponse;
import com.zest.productapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, Token Refresh & Logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "username": "john",
                  "email": "john@example.com",
                  "password": "secret123",
                  "roles": ["ROLE_USER"]
                }
                """))
        )
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully",
                        authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login and obtain JWT tokens",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                { "username": "john", "password": "secret123" }
                """))
        )
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh-token")
    @Operation(
        summary = "Rotate refresh token and get a new access token",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                { "refreshToken": "<your-refresh-token-uuid>" }
                """))
        )
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout – invalidates refresh token AND blacklists the current access token",
        description = "After logout the access token is immediately invalid (Redis-backed denylist) "
                    + "so it cannot be reused even within its remaining TTL."
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        // Extract the raw Bearer token so we can blacklist its JTI
        String rawToken = extractRawToken(httpRequest);
        authService.logout(userDetails.getUsername(), rawToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private String extractRawToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
