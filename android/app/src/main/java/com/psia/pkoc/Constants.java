package com.psia.pkoc;

import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class Constants
{
    final static UUID ServiceUUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    final static UUID ServiceLegacyUUID = UUID.fromString("41fb60a1-d4d0-4ae9-8cbb-b62b5ae81810");
    final static UUID WriteUUID = UUID.fromString("fe278a85-89ae-191f-5dde-841202693835");
    final static UUID ReadUUID = UUID.fromString("e5b1b3b5-3cca-3f76-cd86-a884cc239692");
    final static UUID ConfigUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static ArrayList<ReaderModel> KnownReaders = new ArrayList<>(Collections.singletonList
            (
                    new ReaderModel
                    (
                            TLVProvider.getByteArrayFromGuid(UUID.fromString("ad0cbc8f-c353-427a-b479-37b5efcff6be")),
                            TLVProvider.getByteArrayFromGuid(UUID.fromString("b9897ed0-5272-4341-979a-b69850112d80"))
                    )
            ));

    public static final ArrayList<SiteModel> KnownSites = new ArrayList<>(Collections.singletonList
    (
        new SiteModel
            (
                UUID.fromString("b9897ed0-5272-4341-979a-b69850112d80"),
                Hex.decode("04b71bb4b0de53f06a09ea6c91b483a898645005a30ec9422b95a67908f640abac440b1e4e705db4a626f7ac4e4dcfeba9f7157872446e61f58282c426f4e838af")
            )
    ));
}

