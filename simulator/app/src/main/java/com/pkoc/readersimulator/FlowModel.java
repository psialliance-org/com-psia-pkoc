package com.pkoc.readersimulator;

import android.bluetooth.BluetoothDevice;

import java.security.KeyPair;

public class FlowModel
{
    BluetoothDevice connectedDevice;
    PKOC_ConnectionType connectionType = PKOC_ConnectionType.Uncompressed;
    byte[] publicKey;
    KeyPair transientKeyPair;
    byte[] receivedTransientPublicKey;
    byte[] protocolVersion;
    byte[] sharedSecret;
    byte[] signature;
    int counter = 1;
    int creationTime = 0;
}
