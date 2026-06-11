package com.roadwatch.mobile.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "complaints")
public class ComplaintEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String imagePath;
    public String location;
    public long timestamp;
    public String description;
    public boolean isSynced;
    public String roadType;
    public String severity;
    public Double latitude;
    public Double longitude;

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
