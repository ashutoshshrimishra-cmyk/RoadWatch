package com.roadwatch.mobile.ui.notifications;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.data.NotificationEntity;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnClickListener {
        void onNotificationClick(NotificationEntity item);
    }

    private final List<NotificationEntity> items = new ArrayList<>();
    private final OnClickListener clickListener;

    public NotificationAdapter(OnClickListener listener) {
        this.clickListener = listener;
    }

    public void submit(List<NotificationEntity> next) {
        items.clear();
        if (next != null) items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationEntity item = items.get(position);
        holder.bind(item, clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvBody, tvTime;
        final View unreadDot;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvBody = itemView.findViewById(R.id.tvBody);
            tvTime = itemView.findViewById(R.id.tvTime);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }

        void bind(NotificationEntity item, OnClickListener listener) {
            tvTitle.setText(item.title != null ? item.title : "Notification");
            tvBody.setText(item.body != null ? item.body : "");
            tvTime.setText(DateUtils.getRelativeTimeSpanString(
                    item.receivedAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS));
            unreadDot.setVisibility(item.read ? View.GONE : View.VISIBLE);
            tvTitle.setAlpha(item.read ? 0.7f : 1f);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(item);
            });
        }
    }
}
