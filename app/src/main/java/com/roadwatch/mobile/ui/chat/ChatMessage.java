package com.roadwatch.mobile.ui.chat;

public class ChatMessage {

    public enum Type {
        USER,
        AI,
        TYPING
    }

    public final String text;
    public final Type type;
    public final long timestamp;

    public ChatMessage(String text, Type type) {
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    /** Convenience helper for legacy callers. */
    public ChatMessage(String text, boolean fromUser) {
        this(text, fromUser ? Type.USER : Type.AI);
    }

    public boolean isUser() {
        return type == Type.USER;
    }

    public boolean isTyping() {
        return type == Type.TYPING;
    }
}
