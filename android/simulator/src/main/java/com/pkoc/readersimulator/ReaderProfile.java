package com.pkoc.readersimulator;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;

import com.psia.pkoc.core.Constants;

import java.util.UUID;

/**
 * Reader Profile
 */
public class ReaderProfile
{
    static final int Version = 200;

    /**
     * Create Reader Service
     *
     * @return Bluetooth Gatt Service for PKOC over BLE
     */
    public static BluetoothGattService createReaderService()
    {
        BluetoothGattService service = getBluetoothGattService(Constants.ServiceUUID);
        getBluetoothGattService(Constants.ServiceLegacyUUID);

        return service;
    }

    @NonNull
    private static BluetoothGattService getBluetoothGattService(UUID serviceUUID)
    {
        BluetoothGattService service = new BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic readCharacteristic = new BluetoothGattCharacteristic(Constants.ReadUUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(Constants.ConfigUUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        readCharacteristic.addDescriptor(configDescriptor);

        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(Constants.WriteUUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(readCharacteristic);
        service.addCharacteristic(writeCharacteristic);
        return service;
    }
}
