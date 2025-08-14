package com.psia.pkoc;

import java.util.UUID;

public class SiteModel {
    public final UUID SiteUUID;
    public final byte[] PublicKey;
    private byte[] EphemeralPublicKey;

    public SiteModel(UUID siteUUID, byte[] publicKey) {
        this.SiteUUID = siteUUID;
        this.PublicKey = publicKey;
    }

    public void setEphemeralPublicKey(byte[] key) {
        this.EphemeralPublicKey = key;
    }

    public byte[] getEphemeralPublicKey() {
        return this.EphemeralPublicKey;
    }
}
