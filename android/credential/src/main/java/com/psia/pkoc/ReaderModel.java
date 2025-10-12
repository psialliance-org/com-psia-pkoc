package com.psia.pkoc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

import com.psia.pkoc.core.ReaderDto;

@Entity(
    tableName = "readers",
    primaryKeys = { "siteIdentifier", "readerIdentifier" },
    indices = {
        @Index(value = { "siteIdentifier" })
    }
)
public class ReaderModel
{
    @ColumnInfo(name = "protocolVersion", typeAffinity = ColumnInfo.BLOB)
    @Nullable
    private byte[] _protocolVersion;

    @ColumnInfo(name = "readerTransientPublicKey", typeAffinity = ColumnInfo.BLOB)
    @Nullable
    private byte[] _readerTransientPublicKey;

    @ColumnInfo(name = "readerIdentifier", typeAffinity = ColumnInfo.BLOB)
    @NonNull
    private byte[] _readerIdentifier;

    @ColumnInfo(name = "siteIdentifier", typeAffinity = ColumnInfo.BLOB)
    @NonNull
    private byte[] _siteIdentifier;

    public ReaderModel()
    {
        _readerIdentifier = new byte[16];
        _siteIdentifier = new byte[16];
    }

    public ReaderModel(@NonNull byte[] readerId, @NonNull byte[] siteId)
    {
        _readerIdentifier = readerId;
        _siteIdentifier = siteId;
    }

    @Nullable public byte[] getProtocolVersion() { return _protocolVersion; }
    public void setProtocolVersion(@Nullable byte[] v) { _protocolVersion = v; }

    @Nullable public byte[] getReaderTransientPublicKey() { return _readerTransientPublicKey; }
    public void setReaderTransientPublicKey(@Nullable byte[] v) { _readerTransientPublicKey = v; }

    @NonNull public byte[] getReaderIdentifier() { return _readerIdentifier; }
    public void setReaderIdentifier(@NonNull byte[] v) { _readerIdentifier = v; }

    @NonNull public byte[] getSiteIdentifier() { return _siteIdentifier; }
    public void setSiteIdentifier(@NonNull byte[] v) { _siteIdentifier = v; }

    public ReaderDto toDto()
    {
        return new ReaderDto(
            this._protocolVersion,
            this._readerTransientPublicKey,
            this._readerIdentifier,
            this._siteIdentifier
        );
    }
}
