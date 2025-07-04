package com.psia.pkoc;

import static java.lang.System.arraycopy;
import static com.psia.pkoc.CryptoProvider.CreateTransientKeyPair;
import static com.psia.pkoc.CryptoProvider.GetSignedMessage;
import static com.psia.pkoc.CryptoProvider.getUncompressedPublicKeyBytes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

//import org.bouncycastle.util.Arrays;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Arrays;

/**
 * Bluetooth Gatt Callback for PKOC
 */
public class PKOC_BluetoothCallbackGatt extends BluetoothGattCallback
{
    private final Activity mainActivity;
    private Handler bHandler;
    private final Handler uiHandler;
    private static int handleDatagramsCounter = 0;
    private static int completeTransactionCounter = 0;
    private final FlowModel _flowModel;

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

    @SuppressLint("MissingPermission")
    private void handleDatagrams(BluetoothGatt gatt, byte[] datagrams) {
        handleDatagramsCounter++;
        Log.d("handleDatagrams", "handleDatagrams called " + handleDatagramsCounter + " times");
        Log.d("handleDatagrams", "Received datagrams: " + Arrays.toString(datagrams));

        Log.d("handleDatagrams", "This is getting called for both normal and perfect data flows: " + Arrays.toString(datagrams));
        //This is called in Normal Flow and ECHDE Flow to populate the datagram.
        ArrayList<BLE_Packet> gattTypes = TLVProvider.GetBleValues(datagrams);

        for (int a = 0; a < gattTypes.size(); a++) {
            byte[] data = gattTypes.get(a).Data;
            Log.d("handleDatagrams", "Processing packet type: " + gattTypes.get(a).PacketType + ", data: " + Arrays.toString(data));

            switch (gattTypes.get(a).PacketType) {
                case ProtocolVersion:
                    Log.d("handleDatagrams", "ProtocolVersion data: " + gattTypes.get(a).Data);
                    _flowModel.reader.setProtocolVersion(gattTypes.get(a).Data);
                    break;

                case CompressedTransientPublicKey:
                    Log.d("handleDatagrams", "CompressedTransientPublicKey data: " + gattTypes.get(a).Data);
                    _flowModel.reader.setReaderTransientPublicKey(gattTypes.get(a).Data);
                    break;

                case ReaderLocationIdentifier:
                    Log.d("handleDatagrams", "ReaderLocationIdentifier data: " + gattTypes.get(a).Data);
                    _flowModel.reader.setReaderIdentifier(gattTypes.get(a).Data);
                    break;

                case SiteIdentifier:
                    Log.d("handleDatagrams", "SiteIdentifier data: " + gattTypes.get(a).Data);
                    _flowModel.reader.setSiteIdentifier(gattTypes.get(a).Data);
                    break;

                case Response:
                    Message message = new Message();
                    _flowModel.status = ReaderUnlockStatus.values()[data[0]];
                    message.what = data[0];
                    uiHandler.sendMessage(message);
                    gatt.disconnect();
                    return;

                case DigitalSignature:
                    if (_flowModel.reader == null) {
                        gatt.disconnect();
                        return;
                    }

                    SiteModel siteToFind = null;

                    for (int j = 0; j < Constants.KnownReaders.size(); j++) {
                        if (Arrays.equals(Constants.KnownReaders.get(j).getReaderIdentifier(), _flowModel.reader.getReaderIdentifier())
                                && Arrays.equals(Constants.KnownReaders.get(j).getSiteIdentifier(), _flowModel.reader.getSiteIdentifier())) {
                            for (int k = 0; k < Constants.KnownSites.size(); k++) {
                                if (Arrays.equals(TLVProvider.getByteArrayFromGuid(Constants.KnownSites.get(k).SiteUUID), _flowModel.reader.getSiteIdentifier())) {
                                    siteToFind = Constants.KnownSites.get(k);
                                }
                            }
                        }
                    }

                    if (siteToFind == null) {
                        gatt.disconnect();
                        return;
                    }

                    byte[] originalMessage = generateSignatureMessage();

                    _flowModel.readerValid = CryptoProvider.validateSignedMessage(
                            Objects.requireNonNull(siteToFind).PublicKey,
                            originalMessage,
                            data);

                    if (!_flowModel.readerValid) {
                        Log.d("PKOC_BluetoothCallbackGatt", "Reader not valid");
                        gatt.disconnect();
                        return;
                    }
                    Log.d("PKOC_BluetoothCallbackGatt", "Reader valid");
//              Dhruv this was commented out since it gets called twice, This was added to an if statement below
//                    if (_flowModel.sharedSecret != null) {
//                        Log.d("PKOC_BluetoothCallbackGatt", "Shared secret found, calling completeTransaction");
//                       // completeTransaction(gatt);
//                    }
            }
        }

        if (_flowModel.connectionType == PKOC_ConnectionType.ECHDE_Full) {
            Log.d("FlowType", "Executing ECDHE Perfect Security Flow");
            if (_flowModel.reader == null) {
                Log.d("PKOC_BluetoothCallbackGatt", "Reader not found in perfect security flow");
                _flowModel.status = ReaderUnlockStatus.Unrecognized;
                gatt.disconnect();
                return;
            }
            Log.d("PKOC_BluetoothCallbackGatt", "Reader found in prefect security flow");
            if (_flowModel.reader.getReaderIdentifier() == null) {
                _flowModel.status = ReaderUnlockStatus.Unrecognized;
                gatt.disconnect();
                return;
            }

            if (_flowModel.transientKeyPair == null) {
                _flowModel.transientKeyPair = CreateTransientKeyPair();
                _flowModel.sharedSecret = CryptoProvider.getSharedSecret(
                        _flowModel.transientKeyPair.getPrivate(),
                        _flowModel.reader.getReaderTransientPublicKey());
                Log.d("handleDatagrams", "transientKeyPair was null, creating new one");
                byte[] transientPublicKeyEncoded = _flowModel.transientKeyPair.getPublic().getEncoded();
                byte[] transientPublicKey = getUncompressedPublicKeyBytes(transientPublicKeyEncoded);
                byte[] transientPublicKeyTLV = TLVProvider.GetBleTLV(BLE_PacketType.UncompressedTransientPublicKey, transientPublicKey);

                BluetoothGattService requiredService = tryGetService(gatt); //gatt.getService(Constants.ServiceUUID);

                BluetoothGattCharacteristic reqCharacteristic = requiredService.getCharacteristic(Constants.WriteUUID);
                reqCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                reqCharacteristic.setValue(transientPublicKeyTLV);

                Log.d("handleDatagrams", "Write to GATT server with updated characteristics with the newly created transient key pair in ECDHE Flow: " + Arrays.toString(reqCharacteristic.getValue()));
                //In ECDHE Perfect Security mode we are coming here to send to the client
                gatt.writeCharacteristic(reqCharacteristic);

                return;
            }
        }
        // Dhruv this is changed to include the shared secrete check
        if (_flowModel.reader.getReaderTransientPublicKey() != null || _flowModel.sharedSecret != null) {
            //In Normal Flow this is our first call to that will end up writing to the GATT server
            Log.d("handleDatagrams", "Reader transient public key found, calling completeTransaction");
            // Dhruv this was getting called twice
            completeTransaction(gatt);
        }
    }

    private byte[] generateSignatureMessage()
    {
        if (_flowModel.connectionType == PKOC_ConnectionType.ECHDE_Full)
        {
            Log.d("SignatureMessage", "Went into signature Message Generation");
            byte[] siteIdentifier = _flowModel.reader.getSiteIdentifier();
            byte[] readerIdentifier = _flowModel.reader.getReaderIdentifier();

            byte[] deviceEphemeralPublicKey = _flowModel.transientKeyPair.getPublic().getEncoded();
            byte[] deviceX = CryptoProvider.getPublicKeyComponentX(deviceEphemeralPublicKey);

            byte[] readerPk = _flowModel.reader.getReaderTransientPublicKey();
            byte[] readerX = new byte[32];
            arraycopy(readerPk, 1, readerX, 0, 32);

            return org.bouncycastle.util.Arrays.concatenate(siteIdentifier, readerIdentifier, deviceX, readerX);
        }

        return _flowModel.reader.getReaderTransientPublicKey();
    }

    // Dhruv added function to print out value in Hex
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Use %02x for lowercase hex, or %02X for uppercase
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @SuppressLint("MissingPermission")
    private void completeTransaction (BluetoothGatt gatt)
    {
        Log.d("completeTransaction", "completeTransaction called " + completeTransactionCounter + " times");
        byte[] pkTLVBytes = TLVProvider.GetBleTLV(BLE_PacketType.PublicKey, getUncompressedPublicKeyBytes());

        byte[] toSign = generateSignatureMessage();
        byte[] SignedMessage = GetSignedMessage(toSign);
        byte[] SignatureWithoutASM = TLVProvider.RemoveASNHeaderFromSignature(SignedMessage);
        byte[] SignatureTLV = TLVProvider.GetBleTLV(BLE_PacketType.DigitalSignature, SignatureWithoutASM);

        SharedPreferences sharedPref = mainActivity.getPreferences(Context.MODE_PRIVATE);
        int pkocCreationTime = (int) sharedPref.getLong(PKOC_Preferences.PKOC_CreationTime, System.currentTimeMillis());
        byte[] creationTime = ByteBuffer.allocate(4).putInt(pkocCreationTime).array();
        byte[] creationTimeTLV = TLVProvider.GetBleTLV(BLE_PacketType.LastUpdateTime, creationTime);
        byte[] protocolversionTLV = new byte[]{0x03, 0x00, 0x00, 0x00, 0x01};
        //byte[] secureMessage = org.bouncycastle.util.Arrays.concatenate(pkTLVBytes, SignatureTLV, creationTimeTLV, protocolversionTLV);
        byte[] secureMessage = org.bouncycastle.util.Arrays.concatenate(pkTLVBytes, SignatureTLV, creationTimeTLV);
        if (_flowModel.connectionType == PKOC_ConnectionType.ECHDE_Full && _flowModel.readerValid) {
            Log.d("completeTransaction", "ECHDE Flow build secure message using bouncycastle - BONG");

            // Log the secure message before encryption
            Log.d("completeTransaction", "Secure message before encryption: " + Arrays.toString(secureMessage));
            Log.d("completeTransaction", "Secure message in Hex[] " + bytesToHex(secureMessage));

            // Dhruv this was creating a TLV of the wrong size
//            byte[] encryptTLV = TLVProvider.GetTLV(BLE_PacketType.EncryptedDataFollows, new byte[] { 1 });

            byte[] encrypted = CryptoProvider.getAES256(_flowModel.sharedSecret, secureMessage, _flowModel.Counter);

            Log.d("completeTransaction", "Printing the encrypted data " + bytesToHex(encrypted));

            // Log the encrypted data
            Log.d("completeTransaction", "Encrypted data: " + Arrays.toString(encrypted));


           secureMessage = TLVProvider.GetBleTLV(BLE_PacketType.EncryptedDataFollows, encrypted);

            // Dhruv Commented this out and replaced with line above to return properly formatted TLV
            //secureMessage = org.bouncycastle.util.Arrays.concatenate(encryptTLV, encrypted);


            // Log the final secure message
            Log.d("completeTransaction", "Final secure message: " + Arrays.toString(secureMessage));

            // Dhruv Added a log to log in Hex
            Log.d("completeTransaction", "Final secure message: " + bytesToHex(secureMessage));
        }

        BluetoothGattService requiredService = tryGetService(gatt); //gatt.getService(Constants.ServiceUUID);
        BluetoothGattCharacteristic reqCharacteristic = requiredService.getCharacteristic(Constants.WriteUUID);
        reqCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        reqCharacteristic.setValue(secureMessage);

        Log.d("WriteCharacteristic", "Printing the final value before sending: " + Arrays.toString(secureMessage));

        gatt.writeCharacteristic(reqCharacteristic);
    }

    /**
     * Constructor
     * @param parent Activity
     * @param toUse Enumeration for PKOC flow option
     * @param updateUIHandler Handler to receive UI updates
     */
    public PKOC_BluetoothCallbackGatt (Activity parent, PKOC_ConnectionType toUse, Handler updateUIHandler)
    {
        mainActivity = parent;

        _flowModel = new FlowModel();
        _flowModel.reader = new ReaderModel();
        _flowModel.connectionType = toUse;

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
            /*
            #define  GATT_SUCCESS                        0x0000
            https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h
             */

            if (status != 0x0000 || newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                gatt.close();

                if(_flowModel == null || _flowModel.status == null)
                {
                    Message message = new Message();
                    message.what = ReaderUnlockStatus.Unknown.ordinal();
                    uiHandler.sendMessage(message);
                }

                if (_flowModel != null)
                {
                    if (_flowModel.status == ReaderUnlockStatus.Unrecognized)
                    {
                        Message message = new Message();
                        _flowModel.status = ReaderUnlockStatus.Unrecognized;
                        message.what = ReaderUnlockStatus.Unrecognized.ordinal();
                        uiHandler.sendMessage(message);
                    }
                }
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
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        // This is being called because the server has a change it has notified this client of and will send the data in the characteristic.

        uiHandler.post(() -> {
            byte[] charData = characteristic.getValue();
            Log.d("CharacteristicChanged", "Characteristic data: " + Arrays.toString(charData));

            ArrayList<BLE_Packet> packetsFromMessage = new ArrayList<>();
            int offset2 = 0;

            while (offset2 < charData.length) {
                byte type = charData[offset2];
                int length = charData[offset2 + 1];

                // Ensure length does not exceed the remaining bytes in charData
                if (offset2 + 2 + length > charData.length) {
                    Log.e("CharacteristicChanged", "Invalid length: " + length + " at offset: " + offset2);
                    break;
                }

                if (type == BLE_PacketType.EncryptedDataFollows.getType()) {
                    byte[] encryptedData = new byte[length];
                    System.arraycopy(charData, offset2 + 2, encryptedData, 0, length);
                    Log.d("CharacteristicChanged", "Encrypted data: " + Arrays.toString(encryptedData));
                    byte[] unencryptedData = CryptoProvider.getFromAES256(_flowModel.sharedSecret, encryptedData, _flowModel.Counter);
                    _flowModel.Counter++;
                    Log.d("CharacteristicChanged", "Decrypted data: " + Arrays.toString(unencryptedData));
                    packetsFromMessage.addAll(TLVProvider.GetBleValues(unencryptedData));
                    offset2 += length + 2;
                } else {
                    byte[] data = new byte[length];
                    System.arraycopy(charData, offset2 + 2, data, 0, length);
                    packetsFromMessage.add(new BLE_Packet(BLE_PacketType.decode(type), data));
                    offset2 += length + 2;
                }
            }

            for (BLE_Packet packet : packetsFromMessage) {
                if (packet != null) {
                    switch (packet.PacketType) {
                        case PublicKey:
                            _flowModel.connectionType = PKOC_ConnectionType.Uncompressed;
                            _flowModel.publicKey = packet.Data;
                            Log.d("CharacteristicChanged", "Determined PKOC flow: Normal flow");
                            Log.d("CharacteristicChanged", "Public key: " + Arrays.toString(packet.Data));
                            break;
                        case DigitalSignature:
                            _flowModel.signature = packet.Data;
                            Log.d("CharacteristicChanged", "Signature: " + Arrays.toString(packet.Data));
                            break;
                        case UncompressedTransientPublicKey:
                            Log.d("CharacteristicChanged", "Uncompressed transient public key: " + Arrays.toString(packet.Data));
                            _flowModel.receivedTransientPublicKey = packet.Data;
                            break;
                        case EncryptedDataFollows:
                            Log.w("CharacteristicChanged", "Unexpected EncryptedDataFollows packet");
                            break;
                        case LastUpdateTime:
                            _flowModel.creationTime = new BigInteger(packet.Data).intValue();
                            Log.d("CharacteristicChanged", "Creation time: " + _flowModel.creationTime);
                            break;
                        case ProtocolVersion:
                            _flowModel.protocolVersion = new byte[]{(byte) 0x0C, (byte) 0x03, (short) 0x0000, (short) 0x0001};
                            Log.d("CharacteristicChanged", "ProtocolVersion: " + Arrays.toString(packet.Data));
                        default:
                            break;
                    }
                }
            }

            // Call handleDatagrams after processing the packets
            handleDatagrams(gatt, charData);
        });
    }
}
