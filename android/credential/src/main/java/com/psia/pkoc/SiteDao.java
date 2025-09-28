package com.psia.pkoc;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

@Dao
public interface SiteDao
{
    @Query("SELECT * FROM sites WHERE SiteUUID = :siteId LIMIT 1")
    SiteModel findById(byte[] siteId);

    @Upsert
    void upsert(SiteModel site);

    @Query("SELECT * FROM sites WHERE SiteUUID = :sid LIMIT 1")
    SiteModel get(byte[] sid);

    @Query("SELECT * FROM sites ORDER BY SiteUUID ASC")
    List<SiteModel> list();

    @Query("DELETE FROM sites WHERE SiteUUID = :sid")
    void delete(byte[] sid);
}
