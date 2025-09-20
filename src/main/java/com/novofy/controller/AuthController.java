package com.novofy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.novofy.dto.AuthResponse;
import com.novofy.dto.LoginRequest;
import com.novofy.dto.SignupRequest;
import com.novofy.service.UserService;


import jakarta.servlet.http.HttpServletResponse;


import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private UserService userService;
    private final boolean secure = true; // true in production (HTTPS)

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody SignupRequest signupRequest) {
        try {
            userService.registerUser(signupRequest);
            AuthResponse response = AuthResponse.builder()
                .statusCode(200)
                .error(null)
                .build();
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse response = AuthResponse.builder()
                .statusCode(409)
                .error(e.getMessage())
                .build();
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            AuthResponse response = AuthResponse.builder()
                .statusCode(500)
                .error("Server error")
                .build();
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.loginUser(loginRequest);

            String email = loginRequest.getEmail();
            String username = userService.getUserByEmail(email)
                    .map(user -> user.getUsername())
                    .orElse("");

            AuthResponse authResponse = AuthResponse.builder()
                .statusCode(200)
                .token(token)
                .error(null)
                .username(username)
                .email(email)
                .build();
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            AuthResponse authResponse = AuthResponse.builder()
                .statusCode(401)
                .token(null)
                .error("Login failed: " + e.getMessage())
                .build();
            return ResponseEntity.status(401).body(authResponse);
        } catch (Exception e) {
            AuthResponse authResponse = AuthResponse.builder()
                .statusCode(500)
                .token(null)
                .error("Server error")
                .build();
            return ResponseEntity.status(500).body(authResponse);
        }
    }

    @GetMapping("/google/login")
    public void googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

}
