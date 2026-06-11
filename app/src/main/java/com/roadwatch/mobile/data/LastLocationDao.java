package com.roadwatch.mobile.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface LastLocationDao {

    @Query("SELECT * FROM last_location WHERE id = 1 LIMIT 1")
    LastLocationEntity getLastLocation();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(LastLocationEntity location);
}
