package com.roadwatch.mobile.network.dto;

/**
 * Response body for POST /api/ai/chat.
 * Backend may return either {"reply": "..."} or {"message": "..."} depending
 * on implementation. This DTO accepts both shapes.
 */
public class AiChatResponse {
    public String reply;
    public String message;
    public String response;

    public String getReply() {
        if (reply != null && !reply.isEmpty()) return reply;
        if (message != null && !message.isEmpty()) return message;
        if (response != null && !response.isEmpty()) return response;
        return "";
    }
}
