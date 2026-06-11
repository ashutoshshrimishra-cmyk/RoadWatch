package com.roadwatch.mobile.ui.authorities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.dto.AuthorityDto;

import java.util.ArrayList;
import java.util.List;

public class AuthorityListAdapter extends RecyclerView.Adapter<AuthorityListAdapter.VH> {

    public interface OnContactClickListener {
        void onContactClick(AuthorityDto authority, String type);
    }

    private List<AuthorityDto> authorities = new ArrayList<>();
    private OnContactClickListener listener;

    public void setAuthorities(List<AuthorityDto> list) {
        this.authorities = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnContactClickListener(OnContactClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_authority, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AuthorityDto a = authorities.get(position);
        h.tvName.setText(a.name != null ? a.name : "Unknown");
        h.tvSubtitle.setText(a.getSubtitle());
        h.tvDistrict.setText(a.district != null ? "“ " + a.district : "");
        h.tvPhone.setText(a.phone != null ? "“ " + a.phone : "");
        h.tvEmail.setText(a.email != null ? " " + a.email : "");

        h.tvPhone.setOnClickListener(v -> {
            if (listener != null && a.phone != null) listener.onContactClick(a, "phone");
        });
        h.tvEmail.setOnClickListener(v -> {
            if (listener != null && a.email != null) listener.onContactClick(a, "email");
        });
    }

    @Override
    public int getItemCount() { return authorities.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSubtitle, tvDistrict, tvPhone, tvEmail;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            tvDistrict = v.findViewById(R.id.tvDistrict);
            tvPhone = v.findViewById(R.id.tvPhone);
            tvEmail = v.findViewById(R.id.tvEmail);
        }
    }
}
