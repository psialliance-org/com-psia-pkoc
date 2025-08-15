package com.psia.pkoc;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(
    entities = { SiteModel.class, ReaderModel.class },
    version = 1
)
@TypeConverters({ UuidConverters.class })
public abstract class PKOC_Database extends RoomDatabase
{
    public abstract SiteDao siteDao();
    public abstract ReaderDao readerDao();
}

