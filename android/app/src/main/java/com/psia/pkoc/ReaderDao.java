package com.psia.pkoc;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

@Dao
public interface ReaderDao
{
    @Query("SELECT * FROM readers WHERE readerIdentifier = :readerId AND siteIdentifier = :siteId LIMIT 1")
    ReaderModel findByIds(byte[] readerId, byte[] siteId);

    @Upsert
    void upsert(ReaderModel reader);

    @Query("SELECT * FROM readers WHERE siteIdentifier = :siteId AND readerIdentifier = :readerId LIMIT 1")
    ReaderModel getByIdentity(byte[] siteId, byte[] readerId);

    @Query("SELECT * FROM readers")
    List<ReaderModel> list();

    @Query("SELECT * FROM readers WHERE siteIdentifier = :siteId ORDER BY readerIdentifier ASC")
    List<ReaderModel> listForSite(byte[] siteId);

    @Query("DELETE FROM readers WHERE siteIdentifier = :siteId AND readerIdentifier = :readerId")
    void deleteByIdentity(byte[] siteId, byte[] readerId);

    @Query("DELETE FROM readers WHERE siteIdentifier = :siteId")
    void deleteForSite(byte[] siteId);
}
