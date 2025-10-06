package com.psia.pkoc;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class PKOC_Application extends Application
{
    private static PKOC_Database db;

    @Override
    public void onCreate()
    {
        super.onCreate();
        db = Room.databaseBuilder(getApplicationContext(), PKOC_Database.class, "pkoc.db")
                .build();
    }

    public static PKOC_Database getDb()
    {
        return db;
    }
}
