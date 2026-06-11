package com.roadwatch.mobile.network.dto;

/**
 * Mirrors backend {@code Authority} entity from {@code GET /api/authorities}.
 * Used in the Authority Directory screen.
 */
public class AuthorityDto {
    public Long id;
    public String name;
    public String designation;
    public String zone;
    public String email;
    public String phone;
    public String district;

    /** Display string like "PWD Engineer, Zone 3" */
    public String getSubtitle() {
        StringBuilder sb = new StringBuilder();
        if (designation != null && !designation.isEmpty()) sb.append(designation);
        if (zone != null && !zone.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(zone);
        }
        return sb.length() > 0 ? sb.toString() : "Government Authority";
    }
}
