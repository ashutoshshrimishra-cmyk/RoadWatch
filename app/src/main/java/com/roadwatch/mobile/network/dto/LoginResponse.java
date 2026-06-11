package com.roadwatch.mobile.network.dto;

public class LoginResponse {
    public String token;
    public String tokenType;
    public long expiresIn;

    /** Nested user object returned by /api/citizen/login and /api/citizen/register. */
    public static class User {
        public Long id;
        public String email;
        public String name;
    }

    public User user;

    public String resolveToken() {
        return token;
    }

    public String getToken() { return token; }
    public long getExpiresIn() { return expiresIn; }
    public String getUserName() {
        return user != null ? user.name : null;
    }
}
