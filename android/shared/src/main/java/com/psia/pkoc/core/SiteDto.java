package com.psia.pkoc.core;

import androidx.annotation.NonNull;

public class SiteDto
{
    @NonNull
    public byte[] siteUUID;

    @NonNull
    public byte[] publicKey;

    public SiteDto()
    {
        this.siteUUID = new byte[16];
        this.publicKey = new byte[65];
    }

    public SiteDto(@NonNull byte[] siteUUID, @NonNull byte[] publicKey)
    {
        this.siteUUID = siteUUID;
        this.publicKey = publicKey;
    }
}
