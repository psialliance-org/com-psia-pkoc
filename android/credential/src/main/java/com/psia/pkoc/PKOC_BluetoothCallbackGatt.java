package com.psia.pkoc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.psia.pkoc.core.Constants;
import com.psia.pkoc.core.PKOC_ConnectionType;
import com.psia.pkoc.core.ReaderDto;
import com.psia.pkoc.core.SiteDto;
import com.psia.pkoc.core.interfaces.Transaction;
import com.psia.pkoc.core.transactions.BleEcdheFlowTransaction;
import com.psia.pkoc.core.transactions.BleNormalFlowTransaction;

import java.util.ArrayList;


/**
 * Bluetooth Gatt Callback for PKOC
 */
public class PKOC_BluetoothCallbackGatt extends BluetoothGattCallback
{
    private final Activity mainActivity;
    private Handler bHandler;
    private final Handler uiHandler;
    private Transaction transaction;

    BluetoothGattService requiredService;
    BluetoothGattCharacteristic writeCharacteristic;
    BluetoothGattCharacteristic readCharacteristic;

    private BluetoothGattService tryGetService(BluetoothGatt gatt)
    {
        BluetoothGattService requiredService = gatt.getService(Constants.ServiceUUID);

        if (requiredService == null)
        {
            requiredService = gatt.getService(Constants.ServiceLegacyUUID);
        }

        return requiredService;
    }

    /**
     * Constructor
     * @param parent Activity
     * @param toUse Enumeration for PKOC flow option
     * @param updateUIHandler Handler to receive UI updates
     * @param siteDtos list of known sites
     * @param readerDtos list of known readers
     */
    public PKOC_BluetoothCallbackGatt (Activity parent, PKOC_ConnectionType toUse, Handler updateUIHandler, ArrayList<SiteDto> siteDtos, ArrayList<ReaderDto> readerDtos)
    {
        mainActivity = parent;

        if (toUse == PKOC_ConnectionType.Uncompressed)
        {
            transaction = new BleNormalFlowTransaction(true, mainActivity);
        }
        else if (toUse == PKOC_ConnectionType.ECHDE_Full)
        {
            transaction = new BleEcdheFlowTransaction(true, siteDtos, readerDtos, mainActivity);
        }

        uiHandler = updateUIHandler;

        HandlerThread hThread = new HandlerThread("PKOC_GATT");
        if (!hThread.isAlive())
        {
            hThread.start();
            bHandler = new Handler(hThread.getLooper());
        }
    }

    /**
     * On connection state change
     * @param gatt Bluetooth GATT
     * @param status Status
     * @param newState new State
     */
    @Override
    @SuppressLint("MissingPermission")
    public void onConnectionStateChange (BluetoothGatt gatt, int status, int newState)
    {
        super.onConnectionStateChange(gatt, status, newState);

        bHandler.post(() ->
        {
            if (status != 0x0000 || newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                gatt.close();
            }

            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

                try
                {
                    Thread.sleep(2);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }

                gatt.discoverServices();
            }
        });

    }

    /**
     * on Services Discovered
     * @param gatt Bluetooth GATT
     * @param status Status
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onServicesDiscovered (BluetoothGatt gatt, int status)
    {
        super.onServicesDiscovered(gatt, status);

        bHandler.post(() ->
        {
            requiredService = tryGetService(gatt);

            gatt.requestMtu(512);
        });
    }

    /**
     * on MTU Changed
     * @param gatt Bluetooth GATT
     * @param mtu Maximum transmission unit in bytes
     * @param status status integer
     */
    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status)
    {
        super.onMtuChanged(gatt, mtu, status);

        bHandler.post(() -> characteristicRegistration(gatt));
    }

    /**
     * characteristic Registration
     * @param gatt Bluetooth GATT
     */
    @SuppressLint("MissingPermission")
    public void characteristicRegistration(BluetoothGatt gatt)
    {
        requiredService = tryGetService(gatt);

        if (requiredService == null)
        {
            gatt.disconnect();
            return;
        }

        readCharacteristic = requiredService.getCharacteristic(Constants.ReadUUID);
        writeCharacteristic = requiredService.getCharacteristic(Constants.WriteUUID);

        if(readCharacteristic == null || writeCharacteristic == null)
        {
            gatt.disconnect();
            return;
        }

        gatt.setCharacteristicNotification(readCharacteristic, true);

        BluetoothGattDescriptor descriptor = readCharacteristic.getDescriptor(Constants.ConfigUUID);

        if (descriptor == null)
        {
            Log.d("Failed", "No Notification Support From Reader");
            uiHandler.post(() -> Toast.makeText(mainActivity, "Reader does not support notifications", Toast.LENGTH_SHORT).show());
            gatt.disconnect();
            return;
        }

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    /**
     * on Characteristic Changed
     * @param gatt Bluetooth GATT
     * @param characteristic Characteristic
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        super.onCharacteristicChanged(gatt, characteristic);

        bHandler.post(() ->
        {
            final byte[] value = characteristic.getValue();
            if (value == null)
            {
                return;
            }

            var validationResult = transaction.processNewData(value);
            if(validationResult.isValid)
            {
                var toWrite = transaction.toWrite();
                if (toWrite != null)
                {
                    writeCharacteristic.setValue(toWrite);
                    gatt.writeCharacteristic(writeCharacteristic);
                }
                else
                {
                    Message message = new Message();
                    message.what = transaction.getReaderUnlockStatus().ordinal();
                    uiHandler.sendMessage(message);
                }
            }
            else if (validationResult.cancelTransaction)
            {
                gatt.disconnect();
            }
        });
    }
}
