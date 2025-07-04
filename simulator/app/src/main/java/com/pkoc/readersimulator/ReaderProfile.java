package com.pkoc.readersimulator;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Reader Profile
 */
public class ReaderProfile {
    static final int Version = 200;

    static final UUID ReaderUUID = UUID.fromString("ad0cbc8f-c353-427a-b479-37b5efcff6be");
    static final UUID SiteUUID = UUID.fromString("b9897ed0-5272-4341-979a-b69850112d80");

    static final String SitePrivateKey = "308187020100301306072a8648ce3d020106082a8648ce3d030107046d306b02010104204fd90f4a9c6c6dd6773ddaa578cd7bd82b1fbae2110ff2a8ac927e6be76bc7bba14403420004b71bb4b0de53f06a09ea6c91b483a898645005a30ec9422b95a67908f640abac440b1e4e705db4a626f7ac4e4dcfeba9f7157872446e61f58282c426f4e838af";
    static final String SitePublicKey = "3059301306072a8648ce3d020106082a8648ce3d03010703420004b71bb4b0de53f06a09ea6c91b483a898645005a30ec9422b95a67908f640abac440b1e4e705db4a626f7ac4e4dcfeba9f7157872446e61f58282c426f4e838af";

    /**
     * PKOC Service GUID
     */
    final static UUID ServiceUUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");

    /**
     * PKOC Service GUID
     */
    final static UUID LegacyUUID = UUID.fromString("41fb60a1-d4d0-4ae9-8cbb-b62b5ae81810");
    /**
     * PKOC Write Characteristic GUID
     */
    final static UUID WriteUUID = UUID.fromString("fe278a85-89ae-191f-5dde-841202693835");

    /**
     * PKOC Read Characteristic GUID
     */
    final static UUID ReadUUID = UUID.fromString("e5b1b3b5-3cca-3f76-cd86-a884cc239692");

    /**
     * PKOC Configuration Descriptor GUID
     */
    final static UUID ConfigUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Create Reader Service
     *
     * @return Bluetooth Gatt Service for PKOC over BLE
     */
    public static BluetoothGattService createReaderService() {
        BluetoothGattService service = new BluetoothGattService(ServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic readCharacteristic = new BluetoothGattCharacteristic(ReadUUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(ConfigUUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        readCharacteristic.addDescriptor(configDescriptor);

        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(WriteUUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(readCharacteristic);
        service.addCharacteristic(writeCharacteristic);

// Adding second reader service with LegacyUUID
        BluetoothGattService legacyService = new BluetoothGattService(LegacyUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic legacyReadCharacteristic = new BluetoothGattCharacteristic(ReadUUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattDescriptor legacyConfigDescriptor = new BluetoothGattDescriptor(ConfigUUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        legacyReadCharacteristic.addDescriptor(legacyConfigDescriptor);

        BluetoothGattCharacteristic legacyWriteCharacteristic = new BluetoothGattCharacteristic(WriteUUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        legacyService.addCharacteristic(legacyReadCharacteristic);
        legacyService.addCharacteristic(legacyWriteCharacteristic);

        return service;
    }
    public static class BLEDataElement {
        public static void main(String[] args) {
            // Protocol Identifiers
            byte protocolIdentifier = 0x0C;

            // Specification version
            byte specVersion = 0x03; // Spec version 3

            // Vendor sub version
            short vendorSubVersion = 0x0000; // Sub version 00

            // Feature bits
            short featureBits = 0x0001; // CCM supported (bit 0 set)

            // Create a ByteBuffer with a capacity of 5 bytes
            ByteBuffer buffer = ByteBuffer.allocate(5);
            buffer.put(protocolIdentifier);
            buffer.put(specVersion);
            buffer.putShort(vendorSubVersion);
            buffer.putShort(featureBits);

            // Get the byte array
            byte[] dataElement = buffer.array();

            // Print the byte array in hexadecimal format
            //for (byte b : dataElement) {
            //    System.out.printf("0x%02X ", b);
            }
        }
}
