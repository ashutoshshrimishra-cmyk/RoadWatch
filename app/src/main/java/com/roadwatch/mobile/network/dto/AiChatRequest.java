package com.roadwatch.mobile.network.dto;

/**
 * Request body for POST /api/ai/chat. The Spring Boot backend forwards
 * the message to the Mistral AI model and returns the response.
 */
public class AiChatRequest {
    public String message;

    public AiChatRequest(String message) {
        this.message = message;
    }
}
