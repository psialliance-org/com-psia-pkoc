package com.psia.pkoc.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ReaderDto
{
    @Nullable
    public byte[] protocolVersion;

    @Nullable
    public byte[] readerTransientPublicKey;

    @NonNull
    public byte[] readerIdentifier;

    @NonNull
    public byte[] siteIdentifier;

    public ReaderDto()
    {
        readerIdentifier = new byte[16];
        siteIdentifier = new byte[16];
    }

    public ReaderDto(
        @Nullable byte[] protocolVersion,
        @Nullable byte[] readerTransientPublicKey,
        @NonNull byte[] readerIdentifier,
        @NonNull byte[] siteIdentifier
    ) {
        this.protocolVersion = protocolVersion;
        this.readerTransientPublicKey = readerTransientPublicKey;
        this.readerIdentifier = readerIdentifier;
        this.siteIdentifier = siteIdentifier;
    }
}
