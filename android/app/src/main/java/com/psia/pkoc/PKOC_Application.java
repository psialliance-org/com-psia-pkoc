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
                 .addCallback(new RoomDatabase.Callback()
                 {
                     @Override
                     public void onCreate(@NonNull SupportSQLiteDatabase dbObj)
                     {
                         super.onCreate(dbObj);

                         PKOC_Application.db.getQueryExecutor().execute(() ->
                         {
                             for (SiteModel site : Constants.KnownSites)
                             {
                                 PKOC_Application.db.siteDao().upsert(site);
                             }
                             for (ReaderModel reader : Constants.KnownReaders)
                             {
                                 PKOC_Application.db.readerDao().upsert(reader);
                             }
                         });
                     }
                 })
                 .build();
    }

    public static PKOC_Database getDb()
    {
        return db;
    }
}
