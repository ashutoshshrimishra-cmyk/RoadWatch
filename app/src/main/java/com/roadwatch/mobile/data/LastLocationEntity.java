package com.roadwatch.mobile.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "last_location")
public class LastLocationEntity {

    @PrimaryKey
    public int id = 1;

    public double latitude;
    public double longitude;
    public long updatedAt;
}
