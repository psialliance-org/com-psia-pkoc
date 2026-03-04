package com.psia.pkoc;

import android.app.Application;

import androidx.room.Room;

import org.conscrypt.Conscrypt;

import java.security.Security;

public class PKOC_Application extends Application
{
    private static PKOC_Database db;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        db = Room.databaseBuilder(getApplicationContext(), PKOC_Database.class, "pkoc.db")
                .build();
    }

    public static PKOC_Database getDb()
    {
        return db;
    }
}
