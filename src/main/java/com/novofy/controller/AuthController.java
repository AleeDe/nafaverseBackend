package com.novofy.controller;

import com.novofy.dto.AuthResponse;
import com.novofy.dto.LoginRequest;
import com.novofy.dto.SignupRequest;
import com.novofy.model.User;
import com.novofy.service.UserService;
import com.novofy.config.securityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping(value = "/signup", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> register(@RequestBody SignupRequest signupRequest) {
        try {
            userService.registerUser(signupRequest);
            return ResponseEntity.ok(
                AuthResponse.builder().statusCode(200).error(null).build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(
                AuthResponse.builder().statusCode(409).error(e.getMessage()).build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                AuthResponse.builder().statusCode(500).error("Server error").build()
            );
        }
    }

    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.loginUser(loginRequest);
            String email = loginRequest.getEmail();
            String username = userService.getUserByEmail(email).map(User::getUsername).orElse("");
            return ResponseEntity.ok(
                AuthResponse.builder()
                    .statusCode(200)
                    .token(token)
                    .username(username)
                    .email(email)
                    .error(null)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(
                AuthResponse.builder().statusCode(401).error("Login failed: " + e.getMessage()).build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                AuthResponse.builder().statusCode(500).error("Server error").build()
            );
        }
    }

    // Used by frontend to fetch current user from JWT
    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<?> me() {
        String email = securityConfig.getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).body("Unauthorized");
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("User not found");
        User u = userOpt.get();
        return ResponseEntity.ok(java.util.Map.of(
            "id", u.getId() == null ? null : u.getId().toString(),
            "username", u.getUsername(),
            "email", u.getEmail()
        ));
    }

    // Redirects to Spring Security OAuth2 entry point
    @GetMapping("/google/login")
    public ResponseEntity<Void> googleLogin() {
        return ResponseEntity.status(302)
            .location(URI.create("/oauth2/authorization/google"))
            .build();
    }
}
