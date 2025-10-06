package com.psia.pkoc;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sites")
public class SiteModel
{
    @PrimaryKey
    @NonNull
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] SiteUUID;

    @NonNull
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] PublicKey;

    public SiteModel()
    {
        this.SiteUUID = new byte[16];
        this.PublicKey = new byte[65];
    }

    public SiteModel(@NonNull byte[] siteUUID,
                     @NonNull byte[] publicKey)
    {
        this.SiteUUID = siteUUID;
        this.PublicKey = publicKey;
    }
}
