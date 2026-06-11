package com.roadwatch.mobile.network.dto;

/**
 * Login payload €” backend expects {@code username + password}.
 * Mirrors {@code com.roadwatch.backend.dto.LoginRequest} on the server.
 */
public class LoginRequest {
    public String username;
    public String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
