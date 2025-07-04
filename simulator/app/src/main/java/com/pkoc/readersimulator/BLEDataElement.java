package com.pkoc.readersimulator;

import java.nio.ByteBuffer;

public class BLEDataElement {
    private byte protocolIdentifier;
    private byte specVersion;
    private short vendorSubVersion;
    private short featureBits;

    public BLEDataElement(byte protocolIdentifier, byte specVersion, short vendorSubVersion, short featureBits) {
        this.protocolIdentifier = protocolIdentifier;
        this.specVersion = specVersion;
        this.vendorSubVersion = vendorSubVersion;
        this.featureBits = featureBits;
    }

    public String getFullVersionString() {
        // Create a ByteBuffer with a capacity of 5 bytes
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put(protocolIdentifier);
        buffer.put(specVersion);
        buffer.putShort(vendorSubVersion);
        buffer.putShort(featureBits);

        // Get the byte array
        byte[] dataElement = buffer.array();

        // Convert the byte array to a hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : dataElement) {
            hexString.append(String.format("0x%02X ", b));
        }
        return hexString.toString().trim();
    }

    public static void main(String[] args) {
        // Example usage
        BLEDataElement bleDataElement = new BLEDataElement((byte) 0x0C, (byte) 0x03, (short) 0x0000, (short) 0x0001);
        String fullVersionString = bleDataElement.getFullVersionString();
        System.out.println(fullVersionString);
    }
}
