package com.roadwatch.mobile.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert
    long insert(NotificationEntity entity);

    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC")
    LiveData<List<NotificationEntity>> observeAll();

    @Query("SELECT COUNT(*) FROM notifications WHERE read = 0")
    LiveData<Integer> observeUnreadCount();

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    void markRead(long id);

    @Query("UPDATE notifications SET read = 1")
    void markAllRead();

    @Query("DELETE FROM notifications")
    void clearAll();

    @Query("SELECT COUNT(*) FROM notifications WHERE serverId = :serverId")
    int countByServerId(long serverId);
}
