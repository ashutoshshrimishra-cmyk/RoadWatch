package com.roadwatch.mobile.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ComplaintDao {
    @Insert
    long insert(ComplaintEntity complaint);

    @Update
    void update(ComplaintEntity complaint);

    @Query("SELECT * FROM complaints WHERE isSynced = 0")
    List<ComplaintEntity> getUnsyncedComplaints();

    @Query("UPDATE complaints SET isSynced = 1 WHERE id = :id")
    void markSynced(int id);

    @Query("SELECT COUNT(*) FROM complaints")
    androidx.lifecycle.LiveData<Integer> getTotalComplaintsLiveData();

    @Query("SELECT COUNT(*) FROM complaints WHERE isSynced = 0")
    androidx.lifecycle.LiveData<Integer> getUnsyncedCountLiveData();

    @Query("SELECT * FROM complaints ORDER BY timestamp DESC")
    androidx.lifecycle.LiveData<List<ComplaintEntity>> getRecentCapturesLiveData();

    @Query("SELECT * FROM complaints ORDER BY timestamp DESC")
    List<ComplaintEntity> getAllComplaints();
}
