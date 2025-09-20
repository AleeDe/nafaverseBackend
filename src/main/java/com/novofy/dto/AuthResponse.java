package com.novofy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private int statusCode;
    private String error;
    private String token;
    private String username;
    private String email;
}