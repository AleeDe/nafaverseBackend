package com.novofy.controller;

import com.novofy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserService userService;

    // Endpoint to request password reset
    @PostMapping("/request")
    public ResponseEntity<?> requestReset(@RequestParam String email) {
        userService.sendPasswordResetEmail(email);
        return ResponseEntity.ok("Password reset email sent.");
    }

    // Endpoint to reset password using token
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password has been reset.");
    }
}