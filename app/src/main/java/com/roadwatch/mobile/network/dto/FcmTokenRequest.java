package com.roadwatch.mobile.network.dto;

/**
 * Body for POST /api/users/fcm-token. Backend stores this against the
 * authenticated user so it can target push notifications at this device.
 */
public class FcmTokenRequest {
    public String token;
    public String platform = "android";

    public FcmTokenRequest(String token) {
        this.token = token;
    }
}
