package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.PKOC_EncryptionType;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

public class ProtocolVersionPacket implements TransactionPacket
{
    private byte specificationVersion;
    private short vendorVersion;
    private boolean ccmSupported = true;
    private boolean gcmSupported = false;

    public ProtocolVersionPacket(byte[] data)
    {
        if (data.length == 2)
        {
            vendorVersion = (short)(((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
            return;
        }

        if (data.length == 5)
        {
            specificationVersion = data[0];
            vendorVersion = (short) (((data[1] & 0xFF) << 8) | (data[2] & 0xFF));

            int featureBits = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);

            ccmSupported = (featureBits & 0x0001) != 0;
            gcmSupported = (featureBits & 0x0002) != 0;
        }
    }

    public ProtocolVersionPacket(byte _specificationVersion, short _vendorVersion, boolean _ccmSupported, boolean _gcmSupported)
    {
        specificationVersion = _specificationVersion;
        vendorVersion = _vendorVersion;
        ccmSupported = _ccmSupported;
        gcmSupported = _gcmSupported;
    }

    public byte getSpecificationVersion()
    {
        return specificationVersion;
    }

    public short getVendorVersion()
    {
        return vendorVersion;
    }

    public PKOC_EncryptionType getEncryptionType()
    {
        if (gcmSupported)
        {
            return PKOC_EncryptionType.GCM;
        }

        if (ccmSupported)
        {
            return PKOC_EncryptionType.CCM;
        }

        return PKOC_EncryptionType.NotSpecified;
    }

    public byte[] encode()
    {
        byte[] data = new byte[5];

        data[0] = specificationVersion;

        data[1] = (byte)((vendorVersion >> 8) & 0xFF);
        data[2] = (byte)(vendorVersion & 0xFF);

        int featureBits = 0;
        if (ccmSupported)
        {
            featureBits |= 0x0001;
        }
        if (gcmSupported)
        {
            featureBits |= 0x0002;
        }

        data[3] = (byte)(0);
        data[4] = (byte)(featureBits & 0xFF);

        return data;
    }

    public ValidationResult validate()
    {
        var sizeMismatch = new SizeMismatchResult(encode().length, 2, 5);
        if (sizeMismatch.isValid == false)
        {
            return sizeMismatch;
        }

        return new SuccessResult();
    }
}
