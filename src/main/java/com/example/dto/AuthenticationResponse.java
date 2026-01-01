package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private String role;

    public AuthenticationResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }
}