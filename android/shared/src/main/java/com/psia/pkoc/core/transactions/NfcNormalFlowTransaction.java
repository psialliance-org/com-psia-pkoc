package com.psia.pkoc.core.transactions;

import com.psia.pkoc.core.NFC_Packet;
import com.psia.pkoc.core.NFC_PacketType;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.messages.ReaderResponseMessage;
import com.psia.pkoc.core.validations.SuccessResult;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;

public class NfcNormalFlowTransaction extends NormalFlowTransaction<NFC_Packet>
{
    public static final String SELECT_COMMAND_STRING = "00a4040008a00000089800000100";
    public static final String AUTHENTICATION_COMMAND_PREFIX_STRING = "80800001";
    public static final byte[] SUCCESS_STATUS = Hex.decode("9000");
    public static final byte[] GENERAL_ERROR_STATUS = Hex.decode("6f00");
    public static final String SUPPORTED_PROTOCOL_VERSION = "0100";

    private enum State
    {
        INITIAL,
        AWAITING_AUTHENTICATION,
        SUCCESS,
        FAILED
    }

    private State readerState = State.INITIAL;
    private final boolean isDevice;
    private boolean transactionSuccessful = false; // Used for device mode

    public NfcNormalFlowTransaction(boolean _isDevice)
    {
        super(_isDevice);
        this.isDevice = _isDevice;
    }

    @Override
    public ValidationResult processNewData(byte[] data)
    {
        var packets = TLVProvider.GetNfcValues(data);
        for (var packet : packets)
        {
            var vr = processNewPacket(packet);
            if (!vr.isValid)
            {
                return vr;
            }
        }

        return new SuccessResult();
    }

    public byte[] processDeviceCommand(byte[] command)
    {
        if (Arrays.areEqual(command, Hex.decode(SELECT_COMMAND_STRING)))
        {
            byte[] protocolVersion = Hex.decode(SUPPORTED_PROTOCOL_VERSION);
            byte[] protocolVersionTlv = TLVProvider.GetNfcTLV(NFC_PacketType.ProtocolVersion, protocolVersion);
            return Arrays.concatenate(protocolVersionTlv, SUCCESS_STATUS);
        }

        var apduHex = Hex.toHexString(command);
        if (Hex.toHexString(command).startsWith(AUTHENTICATION_COMMAND_PREFIX_STRING))
        {
            String authCommandHexData = apduHex.substring(AUTHENTICATION_COMMAND_PREFIX_STRING.length() + 2);
            var vr = processNewData(Hex.decode(authCommandHexData));

            if (vr.isValid)
            {
                transactionSuccessful = true;
                return Arrays.concatenate(toWrite(), SUCCESS_STATUS);
            }
        }
        return GENERAL_ERROR_STATUS;
    }

    public void processReaderResponse(byte[] response)
    {
        if (isDevice)
        {
            return;
        }

        switch (readerState)
        {
            case INITIAL:
                byte[] protocolVersion = Hex.decode(SUPPORTED_PROTOCOL_VERSION);
                byte[] expectedResponse = Arrays.concatenate(TLVProvider.GetNfcTLV(NFC_PacketType.ProtocolVersion, protocolVersion), SUCCESS_STATUS);
                if (Arrays.areEqual(response, expectedResponse))
                {
                    readerState = State.AWAITING_AUTHENTICATION;
                }
                else
                {
                    readerState = State.FAILED;
                }
                break;
            case AWAITING_AUTHENTICATION:
                if (response.length > 2 && response[response.length - 2] == SUCCESS_STATUS[0] && response[response.length - 1] == SUCCESS_STATUS[1])
                {
                    byte[] responseData = Arrays.copyOfRange(response, 0, response.length - 2);
                    if (responseData.length > 0)
                    {
                        var vr = processNewData(responseData);
                        if (!vr.isValid)
                        {
                            readerState = State.FAILED;
                            return;
                        }
                    }

                    if (validate().isValid)
                    {
                        readerState = State.SUCCESS;
                    }
                    else
                    {
                        readerState = State.FAILED;
                    }
                }
                else
                {
                    readerState = State.FAILED;
                }
                break;
            default:
                // Do nothing in SUCCESS or FAILED states
                break;
        }
    }

    @Override
    public ValidationResult validate()
    {
        if (!isDevice && currentMessage instanceof ReaderResponseMessage)
        {
            return new SuccessResult();
        }
        return super.validate();
    }

    public byte[] getCommandToWrite()
    {
        if (isDevice)
        {
            return null;
        }

        switch (readerState)
        {
            case INITIAL:
                return Hex.decode(SELECT_COMMAND_STRING);
            case AWAITING_AUTHENTICATION:
                byte[] data = toWrite();
                var prefix = Hex.decode(AUTHENTICATION_COMMAND_PREFIX_STRING);
                ByteBuffer command = ByteBuffer.allocate(data.length + prefix.length + 1);
                command.put(prefix);
                command.put((byte) data.length);
                command.put(data);
                return command.array();
            default:
                return null;
        }
    }

    public boolean isTransactionSuccessful()
    {
        if (isDevice)
        {
            return transactionSuccessful;
        }
        else
        {
            return readerState == State.SUCCESS;
        }
    }
}