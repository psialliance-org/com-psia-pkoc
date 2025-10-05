package com.psia.pkoc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import com.psia.pkoc.core.NFC_PacketType;
import com.psia.pkoc.core.PKOC_Preferences;
import com.psia.pkoc.core.PKOC_TransmissionType;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.transactions.NfcNormalFlowTransaction;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

public class PKOC_HostApduService extends HostApduService
{
    private static final String SELECT_COMMAND = "00a4040008a00000089800000100";
    private static final String AUTHENTICATION_COMMAND_PREFIX = "8080000138";
    private static final String SUPPORTED_PROTOCOL_VERSION = "0100";
    private static final String SUCCESS_STATUS = "9000";
    private static final String GENERAL_ERROR_STATUS = "6f00";

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras)
    {
        byte[] response = Hex.decode(GENERAL_ERROR_STATUS);

        SharedPreferences prefs = getSharedPreferences("MainActivity", Context.MODE_PRIVATE);
        int transmissionTypeInt = prefs.getInt(PKOC_Preferences.PKOC_TransmissionType, PKOC_TransmissionType.BLE.ordinal());
        PKOC_TransmissionType transmissionType = PKOC_TransmissionType.values()[transmissionTypeInt];
        if (transmissionType != PKOC_TransmissionType.NFC)
        {
            return response;
        }

        Log.d("NFC", "Received APDU: " + Hex.toHexString(apdu));
        String apduHex = Hex.toHexString(apdu);

        if (apduHex.equals(SELECT_COMMAND))
        {
            byte[] protocolVersion = Hex.decode(SUPPORTED_PROTOCOL_VERSION);
            byte[] protocolVersionTlv = TLVProvider.GetNfcTLV(NFC_PacketType.ProtocolVersion, protocolVersion);
            response = Arrays.concatenate(protocolVersionTlv, Hex.decode(SUCCESS_STATUS));
            Log.d("NFC", "Response to Select APDU: " + Hex.toHexString(response));
        }

        if (apduHex.startsWith(AUTHENTICATION_COMMAND_PREFIX))
        {
            var normalFlow = new NfcNormalFlowTransaction(true);
            String authCommandHexData = apduHex.substring(AUTHENTICATION_COMMAND_PREFIX.length());
            var vr = normalFlow.processNewData(Hex.decode(authCommandHexData));
            if (vr.isValid)
            {
                response = Arrays.concatenate(normalFlow.toWrite(), Hex.decode(SUCCESS_STATUS));

                Intent intent = new Intent("com.psia.pkoc.CREDENTIAL_SENT");
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
            }
        }

        return response;
    }

    @Override
    public void onDeactivated(int reason)
    {
    }
}
