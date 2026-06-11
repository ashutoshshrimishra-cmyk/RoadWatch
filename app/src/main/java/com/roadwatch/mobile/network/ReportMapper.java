package com.roadwatch.mobile.network;

import com.roadwatch.mobile.data.ComplaintEntity;
import com.roadwatch.mobile.network.dto.GeoPointDto;
import com.roadwatch.mobile.network.dto.ReportDto;

import java.util.ArrayList;
import java.util.List;

public final class ReportMapper {

    private ReportMapper() {}

    public static ReportDto fromLocal(ComplaintEntity entity) {
        ReportDto dto = new ReportDto();
        dto.id = (long) entity.id;
        dto.description = entity.description;
        dto.severity = entity.severity;
        dto.roadType = entity.roadType;
        dto.status = entity.isSynced ? "Synced" : "Pending upload";
        if (entity.latitude != null && entity.longitude != null) {
            GeoPointDto point = new GeoPointDto();
            point.latitude = entity.latitude;
            point.longitude = entity.longitude;
            dto.location = point;
        }
        return dto;
    }

    public static List<ReportDto> fromLocalList(List<ComplaintEntity> entities) {
        List<ReportDto> out = new ArrayList<>();
        if (entities == null) return out;
        for (ComplaintEntity entity : entities) {
            if (entity.latitude != null && entity.longitude != null) {
                out.add(fromLocal(entity));
            }
        }
        return out;
    }
}
