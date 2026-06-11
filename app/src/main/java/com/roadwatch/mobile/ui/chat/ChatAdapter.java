package com.roadwatch.mobile.ui.chat;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;
    private static final int TYPE_TYPING = 3;

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /** Returns index of inserted typing indicator, or -1 if one is already present. */
    public int showTypingIndicator() {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).isTyping()) {
                return -1;
            }
        }
        ChatMessage typing = new ChatMessage("", ChatMessage.Type.TYPING);
        messages.add(typing);
        int position = messages.size() - 1;
        notifyItemInserted(position);
        return position;
    }

    public void hideTypingIndicator() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).isTyping()) {
                messages.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void clearAll() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = messages.get(position);
        if (m.isTyping()) return TYPE_TYPING;
        return m.isUser() ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_USER:
                return new MessageViewHolder(
                        inflater.inflate(R.layout.item_chat_user, parent, false));
            case TYPE_TYPING:
                return new TypingViewHolder(
                        inflater.inflate(R.layout.item_chat_typing, parent, false));
            case TYPE_AI:
            default:
                return new MessageViewHolder(
                        inflater.inflate(R.layout.item_chat_ai, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof MessageViewHolder) {
            ((MessageViewHolder) holder).tvMessage.setText(formatChatText(message.text));
        } else if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).animateDots();
        }
    }

    /** Strip markdown so AI replies don't show raw **bold** or bullet asterisks. */
    static String formatChatText(String text) {
        if (text == null) return "";
        String s = text;
        s = s.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        s = s.replaceAll("\\*([^*\\n]+)\\*", "$1");
        s = s.replaceAll("_([^_\\n]+)_", "$1");
        s = s.replaceAll("(?m)^#{1,6}\\s*", "");
        s = s.replaceAll("(?m)^[-*]\\s+", "- ");
        return s.trim();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).cancelAnimations();
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        final View dot1, dot2, dot3;
        ObjectAnimator a1, a2, a3;

        TypingViewHolder(@NonNull View itemView) {
            super(itemView);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        void animateDots() {
            cancelAnimations();
            a1 = createDotAnimator(dot1, 0);
            a2 = createDotAnimator(dot2, 200);
            a3 = createDotAnimator(dot3, 400);
            a1.start();
            a2.start();
            a3.start();
        }

        void cancelAnimations() {
            if (a1 != null) a1.cancel();
            if (a2 != null) a2.cancel();
            if (a3 != null) a3.cancel();
        }

        private ObjectAnimator createDotAnimator(View dot, long startDelay) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f);
            anim.setDuration(900);
            anim.setStartDelay(startDelay);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setInterpolator(new LinearInterpolator());
            return anim;
        }
    }
}
