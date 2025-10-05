package com.psia.pkoc.core.interfaces;

import com.psia.pkoc.core.ValidationResult;

public interface TransactionMessage<TPacket, TPacketType>
{
    public byte[] encodePackets();
    public ValidationResult processNewPacket(TPacket packet);
    public ValidationResult validate();
}
