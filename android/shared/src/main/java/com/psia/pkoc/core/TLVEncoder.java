package com.psia.pkoc.core;

/**
 * Adapter that abstracts the transport-specific bits:
 *  - how to turn a Type enum into its byte,
 *  - how to decode a byte back to the enum,
 *  - how to construct the concrete packet object for callers.
 */
public interface TLVEncoder<TType, TPacket>
{
    byte toByte(TType type);

    TType decode(byte typeByte);

    TPacket newPacket(TType type, byte[] value);
}
