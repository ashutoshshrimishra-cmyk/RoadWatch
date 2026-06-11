package com.roadwatch.mobile.network.dto;

/** Payload for POST /api/citizen/login and similar citizen auth endpoints. */
public class CitizenAuthRequest {
    public String email;
    public String password;

    public CitizenAuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
