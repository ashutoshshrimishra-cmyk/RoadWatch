package com.roadwatch.mobile.ui.authorities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.dto.AuthorityDto;
import com.roadwatch.mobile.ui.BaseActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Authority Directory €” lists all responsible government authorities.
 * Tap phone/email to call or email directly.
 */
public class AuthorityListActivity extends BaseActivity {

    private RecyclerView rvAuthorities;
    private ProgressBar progressBar;
    private AuthorityListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authority_list);
        setupToolbar("Authority Directory");

        rvAuthorities = findViewById(R.id.rvAuthorities);
        progressBar = findViewById(R.id.progressBar);

        adapter = new AuthorityListAdapter();
        adapter.setOnContactClickListener((authority, type) -> {
            if ("phone".equals(type) && authority.phone != null) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + authority.phone)));
            } else if ("email".equals(type) && authority.email != null) {
                Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + authority.email));
                email.putExtra(Intent.EXTRA_SUBJECT, "Road Complaint €” RoadWatch");
                startActivity(Intent.createChooser(email, "Send email"));
            }
        });
        rvAuthorities.setLayoutManager(new LinearLayoutManager(this));
        rvAuthorities.setAdapter(adapter);

        loadAuthorities();
    }

    private void loadAuthorities() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.api(this).getAuthorities().enqueue(new Callback<List<AuthorityDto>>() {
            @Override
            public void onResponse(Call<List<AuthorityDto>> call, Response<List<AuthorityDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setAuthorities(response.body());
                } else {
                    Toast.makeText(AuthorityListActivity.this,
                            "Failed to load authorities", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AuthorityDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AuthorityListActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
