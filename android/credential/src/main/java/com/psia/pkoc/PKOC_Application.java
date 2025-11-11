package com.psia.pkoc;

import android.app.Application;

import androidx.room.Room;

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
