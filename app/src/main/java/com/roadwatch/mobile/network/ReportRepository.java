package com.roadwatch.mobile.network;

import android.content.Context;

import com.roadwatch.mobile.data.AppDatabase;
import com.roadwatch.mobile.data.ComplaintEntity;
import com.roadwatch.mobile.network.dto.PagedComplaintsDto;
import com.roadwatch.mobile.network.dto.ReportDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Response;

public class ReportRepository {

    private final ApiService apiService;
    private final Context context;

    public ReportRepository(Context context) {
        this.context = context.getApplicationContext();
        apiService = ApiClient.api(this.context);
    }

    public List<ReportDto> fetchReports() throws IOException {
        List<ReportDto> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        List<ComplaintEntity> local = AppDatabase.getDatabase(context)
                .complaintDao()
                .getAllComplaints();
        for (ReportDto localReport : ReportMapper.fromLocalList(local)) {
            String key = keyFor(localReport);
            if (seen.add(key)) {
                merged.add(localReport);
            }
        }

        Response<PagedComplaintsDto> response = apiService.getComplaints(0, 200).execute();
        if (response.isSuccessful() && response.body() != null && response.body().content != null) {
            for (ReportDto serverReport : response.body().content) {
                String key = keyFor(serverReport);
                if (seen.add(key)) {
                    merged.add(serverReport);
                }
            }
            return merged;
        }

        if (!merged.isEmpty()) {
            return merged;
        }

        int code = response.code();
        throw new IOException("Could not load reports from server (HTTP " + code + "). Showing local reports only.");
    }

    private static String keyFor(ReportDto report) {
        if (report.id != null) {
            return "id:" + report.id;
        }
        if (report.location != null) {
            return "loc:" + report.location.latitude + "," + report.location.longitude;
        }
        return "desc:" + (report.description != null ? report.description : "");
    }
}
