package com.psia.pkoc;

import java.util.UUID;

public class SiteModel
{
    public final UUID SiteUUID;
    public final byte[] PublicKey;

    public SiteModel(UUID siteUUID, byte[] publicKey)
    {
        SiteUUID = siteUUID;
        PublicKey = publicKey;
    }
}
