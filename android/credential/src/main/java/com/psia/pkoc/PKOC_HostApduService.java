package com.psia.pkoc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.PKOC_Preferences;
import com.psia.pkoc.core.PKOC_TransmissionType;
import com.psia.pkoc.core.transactions.NfcNormalFlowTransaction;

import org.bouncycastle.util.encoders.Hex;

public class PKOC_HostApduService extends HostApduService
{
    private NfcNormalFlowTransaction normalFlow;

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras)
    {
//        CryptoProvider.initializeCredentials(this);
        SharedPreferences prefs = getSharedPreferences("MainActivity", Context.MODE_PRIVATE);
        int transmissionTypeInt = prefs.getInt(PKOC_Preferences.PKOC_TransmissionType, PKOC_TransmissionType.BLE.ordinal());
        PKOC_TransmissionType transmissionType = PKOC_TransmissionType.values()[transmissionTypeInt];
        if (transmissionType != PKOC_TransmissionType.NFC)
        {
            return NfcNormalFlowTransaction.GENERAL_ERROR_STATUS;
        }

        Log.d("NFC", "Received APDU: " + Hex.toHexString(apdu));

        if (apdu == null)
        {
            return NfcNormalFlowTransaction.GENERAL_ERROR_STATUS;
        }

        if (normalFlow == null)
        {
            normalFlow = new NfcNormalFlowTransaction(true);
        }

        byte[] response = normalFlow.processDeviceCommand(apdu);

        if (normalFlow.isTransactionSuccessful())
        {
            Intent intent = new Intent("com.psia.pkoc.CREDENTIAL_SENT");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }

        Log.d("NFC", "Response APDU: " + Hex.toHexString(response));
        return response;
    }

    @Override
    public void onDeactivated(int reason)
    {
        normalFlow = null;
    }
}
