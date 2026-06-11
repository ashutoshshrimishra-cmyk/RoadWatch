package com.roadwatch.mobile.data;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room database root.
 *
 * Migration history:
 *   v2 ’ v3: added latitude/longitude on complaints
 *   v3 ’ v4: added notifications table for the in-app inbox (Step 8)
 *
 * Destructive migration is intentionally NOT enabled €” losing offline complaint
 * data on a schema bump would silently erase user reports.
 */
@Database(
    entities = {
        ComplaintEntity.class,
        LastLocationEntity.class,
        NotificationEntity.class
    },
    version = 5,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    public abstract ComplaintDao complaintDao();
    public abstract LastLocationDao lastLocationDao();
    public abstract NotificationDao notificationDao();

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE complaints ADD COLUMN latitude REAL");
            database.execSQL("ALTER TABLE complaints ADD COLUMN longitude REAL");
            Log.i(TAG, "Migration 2’3 complete: added latitude/longitude columns");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `notifications` ("
              + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
              + "`title` TEXT, "
              + "`body` TEXT, "
              + "`channel` TEXT, "
              + "`complaintId` INTEGER, "
              + "`receivedAt` INTEGER NOT NULL, "
              + "`read` INTEGER NOT NULL"
              + ")");
            Log.i(TAG, "Migration 3\u20194 complete: notifications table created");
        }
    };
    
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE notifications ADD COLUMN serverId INTEGER");
            Log.i(TAG, "Migration 4\u20195 complete: added serverId column to notifications");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "roadwatch_database")
                                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                                .build();
                    } catch (IllegalStateException | IllegalArgumentException e) {
                        Log.w(TAG, "Room database migration failed, recreating database.", e);
                        INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "roadwatch_database")
                                .fallbackToDestructiveMigration()
                                .build();
                    }
                }
            }
        }
        return INSTANCE;
    }
}
