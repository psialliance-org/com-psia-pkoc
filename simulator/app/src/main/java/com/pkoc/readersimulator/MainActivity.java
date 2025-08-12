package com.pkoc.readersimulator;

import static java.lang.System.arraycopy;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.text.Html;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;


public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = "MainActivity";
    private Button rdrButton;
    private TextView textView;
    private TextView readerLocationUUIDView;
    private TextView readerSiteUUIDView;
    private TextView sitePublicKeyView;
    private TextView nfcAdvertisingStatusView;
    private TextView bleAdvertisingStatusView;
    private String bleStatusValue;
    private String scanReaderUUIDValue;
    private String siteUUIDValue;
    private String sitePublicKeyValue;
    private String nfcAdvertisingStatusValue;
    private String bleAdvertisingStatusValue;

    private NfcAdapter nfcAdapter;

    private BluetoothManager mBluetoothManager;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mBluetoothGattServer;
    private final ArrayList<FlowModel> _connectedDevices = new ArrayList<>();
    private FlowModel currentDeviceModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("onCreate", "onCreate called");
        setContentView(R.layout.activity_main);

        // Set the title of the activity
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("PSIA PKOC Reader Simulator");

        // Set up the reader details button
        rdrButton = findViewById(R.id.rdrButton);
        rdrButton.setVisibility(View.VISIBLE); // Make the button visible
        rdrButton.setOnClickListener(v -> showRdrDetails());

        textView = findViewById(R.id.textView);
        readerLocationUUIDView = findViewById(R.id.readerLocationUUID);
        readerSiteUUIDView = findViewById(R.id.readerSiteUUID);
        sitePublicKeyView = findViewById(R.id.sitePublicKey);
        nfcAdvertisingStatusView = findViewById(R.id.nfcadvertisingStatus);
        bleAdvertisingStatusView = findViewById(R.id.bleadvertisingStatus);

        // Initialize with some values
        scanReaderUUIDValue = "<b>Reader Location UUID:</b>";
        siteUUIDValue = "<b>Reader Site UUID:</b>";
        sitePublicKeyValue = "<b>Site Public Key:</b>";
        nfcAdvertisingStatusValue = "<b>NFC Advertising Status:</b>";
        bleAdvertisingStatusValue = "<b>BLE Advertising Status:</b>";

        displayValues();

        // Set initial text with bold headers
        String initialText = "<b>Scan a PKOC NFC or BLE Credential</b>";
        textView.setText(Html.fromHtml(initialText, Html.FROM_HTML_MODE_LEGACY));

        String readerLocationUUIDText = "<b>Reader Location UUID:</b> " + ReaderProfile.ReaderUUID.toString();
        String readerSiteUUIDText = "<b>Reader Site UUID:</b> " + ReaderProfile.SiteUUID.toString();
        String sitePublicKeyText = "<b>Site Public Key:</b> " + getSitePublicKey();
        String nfcAdvertisingStatusText = "<b>NFC Advertising Status:</b> " + getAdvertisingStatus();
        String bleAdvertisingStatusText = "<b>BLE Advertising Status:</b> Pending";


        readerLocationUUIDView.setText(Html.fromHtml(readerLocationUUIDText, Html.FROM_HTML_MODE_LEGACY));
        readerSiteUUIDView.setText(Html.fromHtml(readerSiteUUIDText, Html.FROM_HTML_MODE_LEGACY));
        sitePublicKeyView.setText(Html.fromHtml(sitePublicKeyText, Html.FROM_HTML_MODE_LEGACY));
        nfcAdvertisingStatusView.setText(Html.fromHtml(nfcAdvertisingStatusText, Html.FROM_HTML_MODE_LEGACY));
        bleAdvertisingStatusView.setText(Html.fromHtml(bleAdvertisingStatusText, Html.FROM_HTML_MODE_LEGACY));

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            textView.setText(R.string.nfc_is_not_available_on_this_device);
            return;
        }

        // Check for Bluetooth and location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADVERTISE,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN,
                        }, 1);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                        }, 1);
            }
        }
        else
        {
            initializeBluetooth();
        }
    }

    @Override
    protected void onDestroy()
    {
        teardownBluetooth();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                initializeBluetooth();
            }
            else
            {
                Log.d("onCreate", "Bluetooth permissions have not been granted.");
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeBluetooth()
    {
        // Initialize Bluetooth
        Log.d("onCreate", "Initializing Bluetooth");
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothAdapter.setName("PSIA Reader Simulator");

        if (!checkBluetoothSupport(mBluetoothAdapter)) {
            Log.d("onCreate", "Bluetooth not supported");
            finish();
        }

        // Start BLE advertising and server
        Log.d("onCreate", "Starting BLE advertising and server");
        startAdvertising();
        startServer();
    }

    private void teardownBluetooth()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                return;
        }
        else
        {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                return;
        }

        if (mBluetoothLeAdvertiser != null)
        {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mBluetoothLeAdvertiser = null;
        }

        if (mBluetoothGattServer != null)
        {
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
            mBluetoothGattServer = null;
        }
    }

    private void displayValues() {
        readerLocationUUIDView.setText(scanReaderUUIDValue);
        readerSiteUUIDView.setText(siteUUIDValue);
        sitePublicKeyView.setText(sitePublicKeyValue);
        nfcAdvertisingStatusView.setText(nfcAdvertisingStatusValue);
        bleAdvertisingStatusView.setText(bleAdvertisingStatusValue);
    }

    private String getSitePublicKey() {
        // Retrieve or generate the Site Public Key
        byte[] sitePubKey = CryptoProvider.getUncompressedPublicKeyBytes(Hex.decode(ReaderProfile.SitePublicKey));
        return Hex.toHexString(sitePubKey);
    }

    private String getAdvertisingStatus() {
        // Retrieve or generate the Advertising Status
        return "Not applicable for NFC"; // NFC does not have an advertising status like BLE
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("onResume", "onResume called");
        if (nfcAdapter != null) {
            Log.d("enableReaderMode", "enableReaderMode called");
            nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
            nfcAdvertisingStatusView.setText(Html.fromHtml("<b>Advertising Status:</b> Not applicable for NFC", Html.FROM_HTML_MODE_LEGACY));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            Log.d("disableReaderMode", "disableReaderMode called");
            nfcAdapter.disableReaderMode(this);
            nfcAdvertisingStatusView.setText(Html.fromHtml("<b>Advertising Status:</b> Disabled", Html.FROM_HTML_MODE_LEGACY));
        }
    }


    @Override
    public void onTagDiscovered(Tag tag) {
        Log.d("NFC", "Tag discovered");
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep != null) {
            try {
                isoDep.connect();
                byte[] selectResponse = sendSelectCommand(isoDep);
                Log.d("NFC", "SELECT Response: " + Hex.toHexString(selectResponse));
                byte[] transactionId = new byte[16]; // Generate or obtain a transaction ID
                new SecureRandom().nextBytes(transactionId); // Ensure it's random
                byte[] readerIdentifier = new byte[32]; // Obtain the reader identifier
                new SecureRandom().nextBytes(readerIdentifier); // Ensure it's random
                byte[] authResponse = sendAuthenticationCommand(isoDep, transactionId, readerIdentifier);
                Log.d("NFC", "Authentication Response: " + Hex.toHexString(authResponse));

                byte[] publicKey = extractPublicKey(authResponse);
                if (publicKey == null) {
                    Log.e(TAG, "Invalid public key extracted");
                    showInvalidKeyDialog();
                    isoDep.close();
                    return;
                }
                byte[] signature = extractSignature(authResponse);
                if (signature == null) {
                    Log.e(TAG, "Invalid signature extracted");
                    showInvalidKeyDialog();
                    isoDep.close();
                    return;
                }
                // Validate the public key using the transaction ID and signature
                if (!isValidPublicKey(publicKey, transactionId, signature)) {
                    Log.e(TAG, "Invalid public key");
                    showInvalidKeyDialog();
                    isoDep.close();
                    return;
                }

                parseAuthenticationResponse(authResponse);
                isoDep.close();
            } catch (IOException e) {
                Log.e("NFC", "Error communicating with NFC tag", e);
            }
        }
    }


    private byte[] sendSelectCommand(IsoDep isoDep) throws IOException {
        return isoDep.transceive(SELECT_APDU);
    }

    private byte[] createAuthenticationCommand(byte[] transactionId, byte[] readerIdentifier) {
        ByteBuffer command = ByteBuffer.allocate(56 + 5); // 5 bytes for CLA, INS, P1, P2, Lc + 56 bytes for data
        command.put((byte) 0x80); // CLA
        command.put((byte) 0x80); // INS
        command.put((byte) 0x00); // P1
        command.put((byte) 0x01); // P2
        command.put((byte) 0x38); // Lc (56 bytes for the data field)
        command.put((byte) 0x5C); // Protocol Version TLV
        command.put((byte) 0x02); // Length
        command.put((byte) 0x01); // Protocol Version
        command.put((byte) 0x00); // Protocol Version
        command.put((byte) 0x4C); // Transaction ID TLV
        command.put((byte) 0x10); // Length (16 bytes)
        command.put(transactionId); // Transaction ID
        command.put((byte) 0x4D); // Reader Identifier TLV
        command.put((byte) 0x20); // Length (32 bytes)
        command.put(readerIdentifier); // Reader Identifier
        return command.array();
    }

    private byte[] sendAuthenticationCommand(IsoDep isoDep, byte[] transactionId, byte[] readerIdentifier) throws IOException {
        byte[] command = createAuthenticationCommand(transactionId, readerIdentifier);
        return isoDep.transceive(command);
    }

    private void parseAuthenticationResponse(byte[] response) {
        // Retrieve the secondary text color from the theme

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textAppearanceSmall, typedValue, true);

        runOnUiThread(() -> {
            int offset = 0;
            while (offset < response.length) {
                byte tag = response[offset++];
                int length = response[offset++];
                byte[] value = Arrays.copyOfRange(response, offset, offset + length);
                offset += length;

                switch (tag) {
                    case (byte) 0x5A:
                        // Public Key


                        String publicKey = Hex.toHexString(value);
                        Log.d("NFC", "Public Key: \n" + publicKey);

                        // Parse the public key
                        if (publicKey.length() == 130) {
                            String header = publicKey.substring(0, 2);
                            String xPortion = publicKey.substring(2, 66);
                            String yPortion = publicKey.substring(66, 130);

                            // Extract 64 Bit and 128 Bit Credentials from X Portion
                            String credential64Bit = xPortion.substring(xPortion.length() - 16);
                            String credential128Bit = xPortion.substring(xPortion.length() - 32);

                            // Convert Hex to Decimal
                            String credential64BitDecimal = new BigInteger(credential64Bit, 16).toString(10);
                            String credential128BitDecimal = new BigInteger(credential128Bit, 16).toString(10);
                            String credential256BitDecimal = new BigInteger(xPortion, 16).toString(10);

                            // Use SpannableStringBuilder to build the final text
                            SpannableStringBuilder formattedText = new SpannableStringBuilder();



                            String connectionTypeText = "Connection Type: Unknown";
                            if (currentDeviceModel.connectionType == PKOC_ConnectionType.ECHDE_Full) {
                                connectionTypeText = "Connection Type: ECDHE Perfect Secrecy Mode";
                            } else if (currentDeviceModel.connectionType == PKOC_ConnectionType.Uncompressed) {
                                connectionTypeText = "Connection Type: Normal Flow";
                            }
                            SpannableString connectionTypeSpannable = new SpannableString(connectionTypeText + "\n\n");
                            connectionTypeSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, connectionTypeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            connectionTypeSpannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, connectionTypeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            connectionTypeSpannable.setSpan(new AbsoluteSizeSpan(16, true), 0, connectionTypeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(connectionTypeSpannable);


                            // Apply bold style to the "Public Key:" text with black color and size 14
                            SpannableString publicKeyHeader = new SpannableString("Public Key: \n");
                            publicKeyHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeyHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeyHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(publicKeyHeader);

                            // Apply colors and font size to the Public Key
                            //Header
                            SpannableStringBuilder publicKeySpannable = new SpannableStringBuilder(publicKey);
                            publicKeySpannable.setSpan(new BackgroundColorSpan(Color.WHITE), 0, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#707173")), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new AbsoluteSizeSpan(14, true), 0, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            //256 bit - xPortion
                            publicKeySpannable.setSpan(new StyleSpan(Typeface.BOLD), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new BackgroundColorSpan(Color.parseColor("#9CC3C9")), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("BLACK")), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            //128 bit
                            publicKeySpannable.setSpan(new StyleSpan(Typeface.ITALIC), 34, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("BLUE")), 34, 50, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Light blue

                            //64 bit
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("YELLOW")), 50, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            //y portion
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#707173")), 66, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            formattedText.append(publicKeySpannable);

                            // Append the rest of the text with specified colors and font size for Headers and values
                            // This is ***AFTER THE PUBIC KEY DISPLAY***

                            SpannableString headerHeader = new SpannableString("\n\nHeader: (Not Used)\n");
                            headerHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            headerHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            headerHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(headerHeader);

                            formattedText.append(applyColorAndSize(header, 0, header.length(), Color.WHITE, (Color.parseColor("#707173")), 14,false));

                            // x-Portion of the public key
                            SpannableString xPortionHeader = new SpannableString("\n\nX Portion 256 Bit HEX: \n");
                            xPortionHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, xPortionHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            xPortionHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, xPortionHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            xPortionHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, xPortionHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(xPortionHeader);

                            formattedText.append(applyColorAndSize(xPortion, 0, xPortion.length(), (Color.parseColor("#9CC3C9")), (Color.parseColor("BLACK")), 14, true));

                            // 256 bit decimal of the public key
                            SpannableString decimalTFSb = new SpannableString("\n\n256 Bit Decimal: \n");
                            decimalTFSb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalTFSb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalTFSb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(decimalTFSb);

                            formattedText.append(applyColorAndSize(credential256BitDecimal, 0, credential256BitDecimal.length(), Color.parseColor("#9CC3C9"), Color.parseColor("BLACK"), 14, true));

                            // 128 bit hex of the public key
                            SpannableString hexOTEb = new SpannableString("\n\n128 Bit HEX: \n");
                            hexOTEb.setSpan(new StyleSpan(Typeface.BOLD), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            hexOTEb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            hexOTEb.setSpan(new AbsoluteSizeSpan(14, true), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(hexOTEb);

                            formattedText.append(applyColorAndSize(credential128Bit, 0, credential128Bit.length(), Color.parseColor("#9CC3C9"), Color.parseColor("BLUE"), 14, true));

                            // 128 bit decimal of the public key
                            SpannableString decimalOTEb = new SpannableString("\n\n128 Bit Decimal: \n");
                            decimalOTEb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalOTEb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalOTEb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(decimalOTEb);

                            formattedText.append(applyColorAndSize(credential128BitDecimal, 0, credential128BitDecimal.length(), Color.parseColor("#9CC3C9"), Color.parseColor("BLUE"),14, true)); // Light blue


                            // 64 bit hex of the public key
                            SpannableString hexSFb = new SpannableString("\n\n64 Bit Hex: \n");
                            hexSFb.setSpan(new StyleSpan(Typeface.BOLD), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            hexSFb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            hexSFb.setSpan(new AbsoluteSizeSpan(14, true), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(hexSFb);

                            formattedText.append(applyColorAndSize(credential64Bit, 0, credential64Bit.length(), Color.parseColor("#9CC3C9"), Color.parseColor("YELLOW"), 14, true));

                            // 64 bit decimal of the public key
                            SpannableString decimalSFb = new SpannableString("\n\n64 Bit Decimal: \n");
                            decimalSFb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalSFb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalSFb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(decimalSFb);

                            formattedText.append(applyColorAndSize(credential64BitDecimal, 0, credential64BitDecimal.length(), Color.parseColor("#9CC3C9"), Color.parseColor("YELLOW"), 14, true));

                            // Y-Portion of the public key
                            SpannableString portionYKey = new SpannableString("\n\nY Portion HEX (Not Used): \n");
                            portionYKey.setSpan(new StyleSpan(Typeface.BOLD), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            portionYKey.setSpan(new ForegroundColorSpan(Color.BLACK), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            portionYKey.setSpan(new AbsoluteSizeSpan(14, true), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(portionYKey);

                            formattedText.append(applyColorAndSize(yPortion, 0, yPortion.length(), Color.WHITE, Color.parseColor("#707173"), 14, false));

                            // Set the formatted text to the TextView
                            textView.setText(formattedText);

                            // Hide reader detail button
                            Button rdrButton = findViewById(R.id.rdrButton);
                            rdrButton.setVisibility(View.GONE);

                            // Set up the email button
                            Button emailButton = findViewById(R.id.emailButton);
                            emailButton.setVisibility(View.VISIBLE); // Make the button visible
                            emailButton.setOnClickListener(v -> sendEmail());

                            // Set up the scan button
                            Button scanButton = findViewById(R.id.scanButton);
                            scanButton.setVisibility(View.VISIBLE); // Make the button visible
                            scanButton.setOnClickListener(v -> resetToScanScreen());
                        } else {
                            // Set the formatted public key if parsing is not applicable
                            SpannableString formattedText = formatText("Public Key: " + publicKey, 14, Color.BLACK);
                            textView.setText(formattedText);
                        }

                        // Hide other fields
                        readerLocationUUIDView.setVisibility(View.GONE);
                        readerSiteUUIDView.setVisibility(View.GONE);
                        sitePublicKeyView.setVisibility(View.GONE);
                        nfcAdvertisingStatusView.setVisibility(View.GONE);
                        bleAdvertisingStatusView.setVisibility(View.GONE);
                        break;

                    case (byte) 0x9E:
                        // Digital Signature
                        String signature = Hex.toHexString(value);
                        Log.d("NFC", "Digital Signature: " + signature);
                        break;

                    case (byte) 0x4C:
                        // Reader Location UUID
                        String readerLocationUUID = Hex.toHexString(value);
                        Log.d("NFC", "Reader Location UUID: " + readerLocationUUID);
                        readerLocationUUIDView.setText(String.format("%s%s", getString(R.string.reader_location_uuid), readerLocationUUID));
                        break;

                    case (byte) 0x4D:
                        // Reader Site UUID
                        String readerSiteUUID = Hex.toHexString(value);
                        Log.d("NFC", "Reader Site UUID: " + readerSiteUUID);
                        readerSiteUUIDView.setText(String.format("%s%s", getString(R.string.reader_site_uuid), readerSiteUUID));
                        break;

                    case (byte) 0x4E:
                        // Site Public Key
                        String sitePublicKey = Hex.toHexString(value);
                        Log.d("NFC", "Site Public Key: " + sitePublicKey);
                        sitePublicKeyView.setText(String.format("%s%s", getString(R.string.site_public_key), sitePublicKey));
                        break;

                    case (byte) 0x4F:
                        // Advertising Status
                        String advertisingStatus = Hex.toHexString(value);
                        Log.d("NFC", "<b>Advertising Status:</b> " + advertisingStatus);
                        nfcAdvertisingStatusView.setText(String.format("%s%s", getString(R.string.b_advertising_status_b), advertisingStatus));
                        break;

                    default:
                        Log.d("NFC", "Unknown TLV: " + tag);
                        break;
                }
            }
        });
    }

    // Define the formatText method
    private SpannableString formatText(String text, int fontSize, int color) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new AbsoluteSizeSpan(fontSize, true), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    // Define the formatText method with HEX color value option
    private SpannableString formatTextH(String text, int textSize, String hexColor) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new AbsoluteSizeSpan(textSize, true), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor(hexColor)), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private static final byte[] SELECT_APDU = {
            (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x08,
            (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x98, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00
    };

    // Method to send email
    private void sendEmail() {
        String emailBody = textView.getText().toString(); // Get the displayed text

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Key and Bit Information");
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetToScanScreen() {
        runOnUiThread(() -> {
            String initialText = "<b>Scan a PKOC BLE or NFC Credential</b>";
            textView.setText(Html.fromHtml(initialText, Html.FROM_HTML_MODE_LEGACY));

            // Hide the email button
            Button emailButton = findViewById(R.id.emailButton);
            emailButton.setVisibility(View.GONE);

            // Hide the scan button
            Button scanButton = findViewById(R.id.scanButton);
            scanButton.setVisibility(View.GONE);

            // Show the reader button
            Button rdrButton = findViewById(R.id.rdrButton);
            rdrButton.setVisibility(View.VISIBLE);

            // Check if the button text is "Show Reader Details"
            if (rdrButton.getText().toString().equals("Show Reader Details")) {
                // Hide the additional fields
                readerLocationUUIDView.setVisibility(View.GONE);
                readerSiteUUIDView.setVisibility(View.GONE);
                sitePublicKeyView.setVisibility(View.GONE);
                nfcAdvertisingStatusView.setVisibility(View.GONE);
                bleAdvertisingStatusView.setVisibility(View.GONE);
            } else {
                // Restore initial values
                scanReaderUUIDValue = "Initial Scan Reader UUID";
                siteUUIDValue = "Initial Site UUID";
                sitePublicKeyValue = "Initial Site Public Key";
                nfcAdvertisingStatusValue = "<b>NFC Advertising Status:</b>";
                bleAdvertisingStatusValue = "<b>BLE Advertising status:</b>";

                displayValues();

                String readerLocationUUIDText = "<b>Reader Location UUID:</b> " + ReaderProfile.ReaderUUID.toString();
                String readerSiteUUIDText = "<b>Reader Site UUID:</b> " + ReaderProfile.SiteUUID.toString();
                String sitePublicKeyText = "<b>Site Public Key:</b> " + getSitePublicKey();
                String nfcAdvertisingStatusText = "<b>Advertising Status:</b> " + getAdvertisingStatus();
                String bleAdvertisingStatusText = "<b>BLE Advertising status:</b> " + bleStatusValue;

                readerLocationUUIDView.setText(Html.fromHtml(readerLocationUUIDText, Html.FROM_HTML_MODE_LEGACY));
                readerSiteUUIDView.setText(Html.fromHtml(readerSiteUUIDText, Html.FROM_HTML_MODE_LEGACY));
                sitePublicKeyView.setText(Html.fromHtml(sitePublicKeyText, Html.FROM_HTML_MODE_LEGACY));
                nfcAdvertisingStatusView.setText(Html.fromHtml(nfcAdvertisingStatusText, Html.FROM_HTML_MODE_LEGACY));
                bleAdvertisingStatusView.setText(Html.fromHtml(bleAdvertisingStatusText, Html.FROM_HTML_MODE_LEGACY));

                // Show other fields
                readerLocationUUIDView.setVisibility(View.VISIBLE);
                readerSiteUUIDView.setVisibility(View.VISIBLE);
                sitePublicKeyView.setVisibility(View.VISIBLE);
                nfcAdvertisingStatusView.setVisibility(View.VISIBLE);
                bleAdvertisingStatusView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideRdrDetails() {
        // Hide the reader details
        readerLocationUUIDView.setVisibility(View.GONE);
        readerSiteUUIDView.setVisibility(View.GONE);
        sitePublicKeyView.setVisibility(View.GONE);
        nfcAdvertisingStatusView.setVisibility(View.GONE);
        bleAdvertisingStatusView.setVisibility(View.GONE);

        // Change button text to "Show Reader Details"
        rdrButton.setText(R.string.show_reader_details);
        // Update OnClickListener to show details
        rdrButton.setOnClickListener(v -> showRdrDetails());
    }

    private void showRdrDetails() {
        // Show the reader details
        readerLocationUUIDView.setVisibility(View.VISIBLE);
        readerSiteUUIDView.setVisibility(View.VISIBLE);
        sitePublicKeyView.setVisibility(View.VISIBLE);
        nfcAdvertisingStatusView.setVisibility(View.VISIBLE);
        bleAdvertisingStatusView.setVisibility(View.VISIBLE);

        // Change button text to "Hide Reader Details"
        rdrButton.setText(R.string.hide_reader_details);
        // Update OnClickListener to hide details
        rdrButton.setOnClickListener(v -> hideRdrDetails());
    }

    // Helper method to apply background, text color, font size, and bold attribute to a specific range of text
    private SpannableStringBuilder applyColorAndSize(String text, int start, int end, int bgColor, int textColor, int fontSize, boolean isBold) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        spannable.setSpan(new BackgroundColorSpan(bgColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(textColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(fontSize, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (isBold) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    private boolean checkBluetoothSupport(BluetoothAdapter mBluetoothAdapter) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }
        return true;
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Advertising started successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertising failed with error code: " + errorCode);
            // Retry advertising if it fails
            if (errorCode != AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                startAdvertising();
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void startAdvertising() {
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothAdapter.setName("PSIA Reader Simulator"); // Set the custom device
        Log.d(TAG, "Starting BLE Advertising");
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            bleStatusValue = "Failed to create advertiser";
            return;
        }
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")))
                .build();
        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID.fromString("41fb60a1-d4d0-4ae9-8cbb-b62b5ae81810")))
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                return;
        } else {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
                return;
        }
        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, mAdvertiseCallback);
        Log.i(TAG, "BLE Advertising status: Successful.");
        bleAdvertisingStatusView.setText(Html.fromHtml("<b>BLE Advertising status:</b> Successful", Html.FROM_HTML_MODE_LEGACY));
        bleStatusValue = "Successful";
    }

    private void startServer ()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                return;
        } else
        {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                return;
        }

        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);

        if (mBluetoothGattServer == null)
        {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mBluetoothGattServer.addService(ReaderProfile.createReaderService());
    }

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                FlowModel newDevice = new FlowModel();
                newDevice.connectedDevice = device;
                _connectedDevices.add(newDevice);

                //layoutClear();
                Log.d(TAG, "Device connected: " + device.getAddress());
                //layoutPost("Device Connected", device.getAddress());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                int toRemove = -1;

                for (int a = 0; a < _connectedDevices.size(); a++) {
                    if (device.getAddress().equals(_connectedDevices.get(a).connectedDevice.getAddress())) {
                        toRemove = a;
                        break;
                    }
                }

                if (toRemove != -1) {
                    _connectedDevices.remove(toRemove);
                }
                Log.d(TAG, "Device disconnected: " + device.getAddress());
                //layoutPost("Device disconnected", device.getAddress());
            }
        }

        public void InitiatePkocFlow(BluetoothDevice device) {
            FlowModel deviceModel = getDeviceCredentialModel(device);

            if (deviceModel == null) {
                return;
            }

            if (deviceModel.transientKeyPair == null && deviceModel.receivedTransientPublicKey == null && deviceModel.signature == null) {
                deviceModel.transientKeyPair = CryptoProvider.CreateTransientKeyPair();
                byte[] encodedPublicKey = Objects.requireNonNull(deviceModel.transientKeyPair).getPublic().getEncoded();
                byte[] uncompressedTransientPublicKey = CryptoProvider.getUncompressedPublicKeyBytes(encodedPublicKey);
                Log.i(TAG, "Uncompressed Transient Public Key: " + Arrays.toString(uncompressedTransientPublicKey));

                byte[] x = new byte[32];
                arraycopy(uncompressedTransientPublicKey, 1, x, 0, 32);
                Log.i(TAG, "X portion of public key: " + Arrays.toString(x));  //Initial communication and X portion to be used for key signature

                byte[] y = new byte[32];
                arraycopy(uncompressedTransientPublicKey, 33, y, 0, 32);
                Log.i(TAG, "Y portion of public key: " + Arrays.toString(y));

                byte[] compressedTransientPublicKey = CryptoProvider.getCompressedPublicKeyBytes(encodedPublicKey);

//                byte[] version = new byte[]{(byte) 0x0C, (byte) 0x03, (short) 0x0000, (short) 0x0001};
                // Dhruv: This is hard set to AES CCM and has 5 bytes of length
                byte[] version = new byte[]{(byte) 0x03, (short) 0x00, (short)0x00, (short) 0x00, (short) 0x01};
                Log.i(TAG, "Version: " + Arrays.toString(version));
                byte[] readerId = TLVProvider.getByteArrayFromGuid(ReaderProfile.ReaderUUID);
                byte[] siteId = TLVProvider.getByteArrayFromGuid(ReaderProfile.SiteUUID);

                byte[] versionTLV = TLVProvider.GetTLV(BLE_PacketType.ProtocolVersion, version);
                byte[] transientPublicKeyTLV = TLVProvider.GetTLV(BLE_PacketType.CompressedTransientPublicKey, compressedTransientPublicKey);
                byte[] readerTLV = TLVProvider.GetTLV(BLE_PacketType.ReaderLocationIdentifier, readerId);
                byte[] siteTLV = TLVProvider.GetTLV(BLE_PacketType.SiteIdentifier, siteId);
                byte[] toSend = org.bouncycastle.util.Arrays.concatenate(versionTLV, transientPublicKeyTLV, readerTLV, siteTLV);
                Log.d(TAG, "Check if we can connect");
                boolean canConnect = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                        canConnect = true;
                } else {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)
                        canConnect = true;
                }
                Log.d(TAG, "canConnect: " + canConnect);
                if (canConnect) {
                    Log.i(TAG, "Message sent in response for request to read PKOC read characteristic");
                    // Start the timeout timer
                    timeoutHandler.postDelayed(timeoutRunnable, 10000); //change this back to 1000 when done troubleshooting
                    writeToReadCharacteristic(device, toSend, false);
                } else {
                    Log.w(TAG, "Not able to connect, nothing has been sent");
                }
            }
        }

        @Override
        public void onDescriptorWriteRequest (BluetoothDevice device,
                                              int requestId,
                                              BluetoothGattDescriptor descriptor,
                                              boolean preparedWrite,
                                              boolean responseNeeded,
                                              int offset,
                                              byte[] value)
        {
            if (ReaderProfile.ConfigUUID.equals(descriptor.getUuid()))
            {
                if (responseNeeded)
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    {
                        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                            return;
                    }
                    else
                    {
                        if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                            return;
                    }

                    boolean success = mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);

                    if (success)
                    {
                        InitiatePkocFlow(device);
                    }
                }
            }
            else
            {
                Log.w(TAG, "Unknown descriptor write request");
                if (responseNeeded)
                {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            enqueueGattOperation(() -> {
                Log.d(TAG, "Characteristic Write Request: " + characteristic.getUuid());
                Log.d(TAG, "Received value: " + Arrays.toString(value));
                Log.d(TAG, "Received value size: " + value.length);
                FlowModel deviceModel = getDeviceCredentialModel(device);
                if (deviceModel == null) {
                    Log.w(TAG, "Device model not found for device: " + device.getAddress());
                    if (responseNeeded) {
                        mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
                    }
                    return;
                }

                // Check if the data is encrypted
                ArrayList<BLE_Packet> packetsFromMessage = TLVProvider.GetValues(value);
                if (deviceModel.connectionType == PKOC_ConnectionType.ECHDE_Full)
                {
                    ArrayList<BLE_Packet> packetsFromEncryptedBlock = new ArrayList<>();

                    for (BLE_Packet blePacket : packetsFromMessage)
                    {
                        if (blePacket.PacketType.getType() == BLE_PacketType.EncryptedDataFollows.getType())
                        {
                            Log.d(TAG, "Encrypted data: " + Hex.toHexString(blePacket.Data));
                            byte[] unencryptedData = CryptoProvider.getFromAES256(deviceModel.sharedSecret, blePacket.Data, deviceModel.counter);
                            if (unencryptedData == null)
                            {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: Failed to decrypt message.", Toast.LENGTH_LONG).show());
                                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
                                onGattOperationCompleted();
                                return;
                            }
                            deviceModel.counter++;
                            Log.d(TAG, "Decrypted data: " + Hex.toHexString(unencryptedData));
                            packetsFromEncryptedBlock.addAll(TLVProvider.GetValues(unencryptedData));
                        }
                    }

                    packetsFromMessage.addAll(packetsFromEncryptedBlock);
                }

                // Use processedValue for TLVProvider.GetValues
                for (BLE_Packet packet : packetsFromMessage) {
                    if (packet != null) {
                        switch (packet.PacketType) {
                            case PublicKey:
                                deviceModel.connectionType = PKOC_ConnectionType.Uncompressed;
                                deviceModel.publicKey = packet.Data;
                                Log.d(TAG, "Determined PKOC flow: Normal flow");
                                Log.d(TAG, "THIS IS THE ONE Public key: " + Hex.toHexString(packet.Data));
                                break;
                            case DigitalSignature:
                                deviceModel.signature = packet.Data;
                                Log.d(TAG, "Signature: " + Hex.toHexString(packet.Data)); //signed by the private key of the device
                                break;
                            case UncompressedTransientPublicKey:
                                Log.d(TAG, "Uncompressed transient public key: " + Hex.toHexString(packet.Data));
                                deviceModel.receivedTransientPublicKey = packet.Data;
                                break;
                            case EncryptedDataFollows:
                                // This case should not be reached because we already decrypted the data above
                                Log.w(TAG, "Unexpected EncryptedDataFollows packet");
                                break;
                            case LastUpdateTime:
                                deviceModel.creationTime = new BigInteger(packet.Data).intValue();
                                Log.d(TAG, "Creation time: " + deviceModel.creationTime);
                                break;
                            case ProtocolVersion:
                                // Dhruv changed this to support 5 byte protocol version
                                deviceModel.protocolVersion = new byte[]{(byte) 0x03, (byte) 0x00, (byte)0x00, (byte) 0x00, (byte)0x01};
                                Log.d(TAG, "Protocol Version is:" + Arrays.toString(deviceModel.protocolVersion));
                            default:
                                break;
                        }
                    }
                }

                //check to validate if we can do the normal flow or need the more secure.  This will happen if the credential is sent in ECDHE Flow (Perfect Security)
                if (deviceModel.publicKey == null && deviceModel.signature == null) {
                    if (deviceModel.receivedTransientPublicKey != null) {
                        deviceModel.sharedSecret = CryptoProvider.getSharedSecret(
                                deviceModel.transientKeyPair.getPrivate(),
                                deviceModel.receivedTransientPublicKey);

                        if (deviceModel.sharedSecret == null)
                        {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: Failed to establish secure channel.", Toast.LENGTH_LONG).show());
                            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
                            onGattOperationCompleted();
                            return;
                        }

                        deviceModel.connectionType = PKOC_ConnectionType.ECHDE_Full;
                        Log.d(TAG, "Determined PKOC flow: ECDHE Perfect Secrecy");
                        Log.d(TAG, "Shared Secret: " + Hex.toHexString(deviceModel.sharedSecret));
                    }

                    if (deviceModel.receivedTransientPublicKey != null && deviceModel.publicKey == null) {
                        byte[] toSign = generateSignaturePackage(deviceModel);

                        byte[] signatureASN = CryptoProvider.GetSignedMessage(toSign);
                        if (signatureASN == null)
                        {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: Failed to sign message.", Toast.LENGTH_LONG).show());
                            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
                            onGattOperationCompleted();
                            return;
                        }

                        Log.d(TAG, "Signature with ASN header: " + Hex.toHexString(signatureASN));

                        byte[] signature = TLVProvider.RemoveASNHeaderFromSignature(signatureASN);
                        Log.d(TAG, "Signature generated: " + Hex.toHexString(signature));

                        byte[] signatureTLV = TLVProvider.GetTLV(BLE_PacketType.DigitalSignature, signature);
                        Log.d(TAG, "Message sent to connected device: " + Hex.toHexString(signatureTLV));
                        writeToReadCharacteristic(device, signatureTLV, false);
                    }
                }
// Check for standard flow
                if (deviceModel.transientKeyPair != null
                        && deviceModel.publicKey != null
                        && deviceModel.signature != null) {
                    byte[] pubKey = deviceModel.publicKey;
                    byte[] signature = deviceModel.signature;

                    byte[] x = new byte[32];
                    System.arraycopy(pubKey, 1, x, 0, 32);
                    Log.d(TAG, "X portion of public key (the credential): " + Hex.toHexString(x));  // This is the credential

                    byte[] y = new byte[32];
                    System.arraycopy(pubKey, 33, y, 0, 32);
                    Log.d(TAG, "Y portion of public key: " + Hex.toHexString(y));

                    byte[] pkoc = new byte[8];
                    System.arraycopy(x, x.length - 8, pkoc, 0, 8);
                    Log.d(TAG, "Last eight bytes of the X portion of the public key: " + Hex.toHexString(pkoc));

                    BigInteger cardNumber64 = new BigInteger(1, pkoc);
                    Log.d(TAG, "64 bit PKOC Credential: " + cardNumber64);

                    byte[] r = new byte[32];
                    System.arraycopy(signature, 0, r, 0, 32);
                    Log.d(TAG, "R portion of signature: " + Hex.toHexString(r));

                    byte[] s = new byte[32];
                    System.arraycopy(signature, 32, s, 0, 32);
                    Log.d(TAG, "S portion of signature: " + Hex.toHexString(s));


// Parse the public key on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        String publicKeyHex = Hex.toHexString(pubKey).toUpperCase();
                        if (publicKeyHex.length() == 130) {
                            String header = publicKeyHex.substring(0, 2);
                            String xPortion = publicKeyHex.substring(2, 66);
                            String yPortion = publicKeyHex.substring(66, 130);

                            // Extract 64 Bit and 128 Bit Credentials from X Portion
                            String credential64Bit = xPortion.substring(xPortion.length() - 16);
                            String credential128Bit = xPortion.substring(xPortion.length() - 32);

                            // Convert Hex to Decimal
                            String credential64BitDecimal = new BigInteger(credential64Bit, 16).toString(10);
                            String credential128BitDecimal = new BigInteger(credential128Bit, 16).toString(10);
                            String credential256BitDecimal = new BigInteger(xPortion, 16).toString(10);

                            // Use SpannableStringBuilder to build the final text
                            SpannableStringBuilder formattedText = new SpannableStringBuilder();

                            currentDeviceModel = deviceModel;

                            String connectionTypeText = "Connection Type: Unknown";
                            if (deviceModel.connectionType == PKOC_ConnectionType.ECHDE_Full) {
                                connectionTypeText = "Connection Type: ECDHE Perfect Secrecy Mode";
                            } else if (deviceModel.connectionType == PKOC_ConnectionType.Uncompressed) {
                                connectionTypeText = "Connection Type: Normal Flow";
                            }
                            SpannableString connectionTypeSpannable = new SpannableString(connectionTypeText + "\n\n");
                            connectionTypeSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, connectionTypeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            connectionTypeSpannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, connectionTypeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            connectionTypeSpannable.setSpan(new AbsoluteSizeSpan(16, true), 0, connectionTypeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(connectionTypeSpannable);


                            // Apply bold style to the "Public Key:" text with black color and size 14
                            SpannableString publicKeyHeader = new SpannableString("Public Key: \n");
                            publicKeyHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeyHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeyHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(publicKeyHeader);

                            // Apply colors and font size to the Public Key
                            SpannableStringBuilder publicKeySpannable = new SpannableStringBuilder(publicKeyHex);
                            publicKeySpannable.setSpan(new BackgroundColorSpan(Color.WHITE), 0, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#707173")), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new AbsoluteSizeSpan(14, true), 0, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            // 256 bit - xPortion
                            publicKeySpannable.setSpan(new StyleSpan(Typeface.BOLD), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new BackgroundColorSpan(Color.parseColor("#9CC3C9")), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("BLACK")), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            // 128 bit
                            publicKeySpannable.setSpan(new StyleSpan(Typeface.ITALIC), 34, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("BLUE")), 34, 50, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Light blue

                            // 64 bit
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("YELLOW")), 50, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            // y portion
                            publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#707173")), 66, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            formattedText.append(publicKeySpannable);

                            // Append the rest of the text with specified colors and font size for Headers and values
                            // This is ***AFTER THE PUBLIC KEY DISPLAY***

                            SpannableString headerHeader = new SpannableString("\n\nHeader: (Not Used)\n".toUpperCase());
                            headerHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            headerHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            headerHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(headerHeader);

                            formattedText.append(applyColorAndSize(header.toUpperCase(), 0, header.length(), Color.WHITE, Color.parseColor("#707173"), 14, false));

                            // x-Portion of the public key
                            SpannableString xPortionHeader = new SpannableString("\n\nX Portion 256 Bit HEX: \n".toUpperCase());
                            xPortionHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, xPortionHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            xPortionHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, xPortionHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            xPortionHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, xPortionHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(xPortionHeader);

                            formattedText.append(applyColorAndSize(xPortion.toUpperCase(), 0, xPortion.length(), Color.parseColor("#9CC3C9"), Color.parseColor("BLACK"), 14, true));

                            // 256 bit decimal of the public key
                            SpannableString decimalTFSb = new SpannableString("\n\n256 Bit Decimal: \n".toUpperCase());
                            decimalTFSb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalTFSb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalTFSb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(decimalTFSb);

                            formattedText.append(applyColorAndSize(credential256BitDecimal, 0, credential256BitDecimal.length(), Color.parseColor("#9CC3C9"), Color.parseColor("BLACK"), 14, true));

                            // 128 bit hex of the public key
                            SpannableString hexOTEb = new SpannableString("\n\n128 Bit HEX: \n".toUpperCase());
                            hexOTEb.setSpan(new StyleSpan(Typeface.BOLD), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            hexOTEb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            hexOTEb.setSpan(new AbsoluteSizeSpan(14, true), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(hexOTEb);

                            formattedText.append(applyColorAndSize(credential128Bit.toUpperCase(), 0, credential128Bit.length(), Color.parseColor("#9CC3C9"), Color.parseColor("BLUE"), 14, true));

                            // 128 bit decimal of the public key
                            SpannableString decimalOTEb = new SpannableString("\n\n128 Bit Decimal: \n".toUpperCase());
                            decimalOTEb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalOTEb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalOTEb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(decimalOTEb);

                            formattedText.append(applyColorAndSize(credential128BitDecimal, 0, credential128BitDecimal.length(), Color.parseColor("#9CC3C9"), Color.parseColor("BLUE"), 14, true));

                            // 64 bit hex of the public key
                            SpannableString hexSFb = new SpannableString("\n\n64 Bit Hex: \n".toUpperCase());
                            hexSFb.setSpan(new StyleSpan(Typeface.BOLD), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            hexSFb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            hexSFb.setSpan(new AbsoluteSizeSpan(14, true), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(hexSFb);

                            formattedText.append(applyColorAndSize(credential64Bit.toUpperCase(), 0, credential64Bit.length(), Color.parseColor("#9CC3C9"), Color.parseColor("YELLOW"), 14, true));

                            // 64 bit decimal of the public key
                            SpannableString decimalSFb = new SpannableString("\n\n64 Bit Decimal: \n".toUpperCase());
                            decimalSFb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalSFb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            decimalSFb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(decimalSFb);

                            formattedText.append(applyColorAndSize(credential64BitDecimal, 0, credential64BitDecimal.length(), Color.parseColor("#9CC3C9"), Color.parseColor("YELLOW"), 14, true));

                            // Y-Portion of the public key
                            SpannableString portionYKey = new SpannableString("\n\nY Portion HEX (Not Used): \n".toUpperCase());
                            portionYKey.setSpan(new StyleSpan(Typeface.BOLD), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            portionYKey.setSpan(new ForegroundColorSpan(Color.BLACK), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            portionYKey.setSpan(new AbsoluteSizeSpan(14, true), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedText.append(portionYKey);

                            formattedText.append(applyColorAndSize(yPortion.toUpperCase(), 0, yPortion.length(), Color.WHITE, Color.parseColor("#707173"), 14, false));
                        // Set the formatted text to the TextView
                        textView.setText(formattedText);
                        // Hide reader detail button
                        Button rdrButton = findViewById(R.id.rdrButton);
                        rdrButton.setVisibility(View.GONE);

                        // Set up the email button
                        Button emailButton = findViewById(R.id.emailButton);
                        emailButton.setVisibility(View.VISIBLE); // Make the button visible
                        emailButton.setOnClickListener(v -> sendEmail());

                        // Set up the scan button
                        Button scanButton = findViewById(R.id.scanButton);
                        scanButton.setVisibility(View.VISIBLE); // Make the button visible
                        scanButton.setOnClickListener(v -> resetToScanScreen());

                        // Hide other fields
                        readerLocationUUIDView.setVisibility(View.GONE);
                        readerSiteUUIDView.setVisibility(View.GONE);
                        sitePublicKeyView.setVisibility(View.GONE);
                        nfcAdvertisingStatusView.setVisibility(View.GONE);
                        bleAdvertisingStatusView.setVisibility(View.GONE);
                    }
                    });
                    boolean sigValid = false;

                    BigInteger xi = new BigInteger(1, x);
                    BigInteger yi = new BigInteger(1, y);
                    BigInteger ri = new BigInteger(1, r);
                    BigInteger si = new BigInteger(1, s);

                    try {
                        ECDomainParameters ecParams = CryptoProvider.getDomainParameters();

                        ECPoint ecPoint = ecParams.getCurve().createPoint(xi, yi);
                        ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(ecPoint, ecParams);

                        ECDSASigner ecSign = new ECDSASigner();
                        ecSign.init(false, pubKeyParams);

                        // Dhruv Added check to correctly identify connection type when going through ECDHE flow when the onCharacteristicWriteRequest is called the second time
                        if(deviceModel.sharedSecret != null && deviceModel.signature != null && deviceModel.publicKey != null)
                        {
                            deviceModel.connectionType = PKOC_ConnectionType.ECHDE_Full;
                            Log.d(TAG, "Determined PKOC flow: ECDHE Perfect Secrecy");
                        }

                        byte[] signatureMessage = generateSignaturePackage(deviceModel);
                        final byte[] hash = CryptoProvider.getSHA256(signatureMessage);
                        if (hash == null) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: Failed to prepare data for verification.", Toast.LENGTH_LONG).show());
                        } else {
                            sigValid = ecSign.verifySignature(hash, ri, si);
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: Signature verification failed.", Toast.LENGTH_LONG).show());
                        Log.d(TAG, e.toString());
                    }

                    byte response = 0x00;

                    boolean cardReadSuccess = Math.abs(cardNumber64.longValue()) > 0;

                    if (cardReadSuccess && sigValid)
                        response = BigInteger.valueOf(ReaderUnlockStatus.AccessGranted.ordinal()).byteValue();
                    else if (cardReadSuccess)
                        response = BigInteger.valueOf(ReaderUnlockStatus.SignatureInvalid.ordinal()).byteValue();

                    byte[] responseTLV = TLVProvider.GetTLV(BLE_PacketType.Response, new byte[]{response});
                    Log.d(TAG, "Message sent to connected device: " + Hex.toHexString(responseTLV));
                    writeToReadCharacteristic(device, responseTLV, true);
                    boolean finalSigValid = sigValid;
                    new Handler(getMainLooper()).post(() -> {
                        if (finalSigValid) {
                            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_RING, 100);
                            toneGen1.startTone(ToneGenerator.TONE_SUP_DIAL, 150);
                        } else {
                            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.ERROR, 100);
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 150);
                        }
                    });
                }

                Log.d(TAG, "Calling onGattOperationCompleted from onCharacteristicWriteRequest");
                onGattOperationCompleted();

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            });
        }

        private FlowModel getDeviceCredentialModel(BluetoothDevice device) {
            for (int a = 0; a < _connectedDevices.size(); a++) {
                FlowModel connectedDevice = _connectedDevices.get(a);

                if (connectedDevice.connectedDevice.getAddress().equals((device.getAddress()))) {
                    return connectedDevice;
                }
            }

            return null;
        }

        private void writeToReadCharacteristic(BluetoothDevice device, byte[] toWrite, boolean cancelConnectionAfterCompleted) {
            boolean canConnect = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                    canConnect = true;
            } else {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)
                    canConnect = true;
            }

            if (canConnect) {
                BluetoothGattCharacteristic readCharacteristic = null;

                // Try to get the characteristic from the primary service
                BluetoothGattService primaryService = mBluetoothGattServer.getService(ReaderProfile.ServiceUUID);
                if (primaryService != null) {
                    readCharacteristic = primaryService.getCharacteristic(ReaderProfile.ReadUUID);
                }

                // If not found, try to get the characteristic from the legacy service
                if (readCharacteristic == null) {
                    BluetoothGattService legacyService = mBluetoothGattServer.getService(ReaderProfile.LegacyUUID);
                    if (legacyService != null) {
                        readCharacteristic = legacyService.getCharacteristic(ReaderProfile.ReadUUID);
                    }
                }

                if (readCharacteristic != null) {
                    readCharacteristic.setValue(toWrite);

                    boolean notified = mBluetoothGattServer.notifyCharacteristicChanged(device, readCharacteristic, false);

                    Log.d(TAG, "Notified: " + notified);
                    //layoutPost("Notify characteristic changed", String.valueOf(notified));

                    if (cancelConnectionAfterCompleted) {
                        mBluetoothGattServer.cancelConnection(device);
                    }
                } else {
                    // Handle the case where neither characteristic is found
                    // For example, log an error or notify the user
                    Log.d(TAG,"Neither characteristic was found!");
                }
            }
        }
/*
The generateSignaturePackage method aligns with the ECDHE Perfect Forward Secrecy Flow specification.
It correctly retrieves and concatenates the required data (site identifier, reader identifier, device ephemeral public key X component, and reader ephemeral public key X component)
before returning the concatenated byte array for signing
 */
        private byte[] generateSignaturePackage(FlowModel deviceModel) {
            if (deviceModel.connectionType == PKOC_ConnectionType.ECHDE_Full) {

                Log.d("GenerateSignaturePackage", "Went into ECDHEFULL signature generation");
                byte[] siteIdentifier = TLVProvider.getByteArrayFromGuid(ReaderProfile.SiteUUID);
                Log.d("NFC", "Site identifier: " + Hex.toHexString(siteIdentifier));

                byte[] readerIdentifier = TLVProvider.getByteArrayFromGuid((ReaderProfile.ReaderUUID));
                Log.d("NFC", "Reader identifier: " + Hex.toHexString(readerIdentifier));

                byte[] deviceEphemeralPublicKey = deviceModel.receivedTransientPublicKey;
                Log.d("NFC", "Device ephemeral public key: " + Hex.toHexString(deviceEphemeralPublicKey));

                byte[] deviceX = new byte[32];
                arraycopy(deviceEphemeralPublicKey, 1, deviceX, 0, 32);
                Log.d("NFC", "Device ephemeral public key x component: " + Hex.toHexString(deviceX));

                byte[] readerPk = deviceModel.transientKeyPair.getPublic().getEncoded();
                Log.d("NFC", "Reader ephemeral public key: " + Hex.toHexString(readerPk));

                byte[] readerX = CryptoProvider.getPublicKeyComponentX(readerPk);
                Log.d("NFC", "Reader ephemeral public key x component: " + Hex.toHexString(readerX));

                byte[] toSign = org.bouncycastle.util.Arrays.concatenate(siteIdentifier, readerIdentifier, deviceX, readerX);
                Log.d("NFC", "Message to sign: " + Hex.toHexString(toSign));

                return toSign;
            }

            byte[] toSignNormalFlow = CryptoProvider.getCompressedPublicKeyBytes(deviceModel.transientKeyPair.getPublic().getEncoded());
            Log.d("NFC", "Message to sign: " + Hex.toHexString(toSignNormalFlow));
            //layoutPost("Message to sign", Hex.toHexString(toSignNormalFlow));

            return toSignNormalFlow;
        }
    };

    private final Queue<Runnable> gattOperationQueue = new LinkedList<>();
    private boolean isGattOperationInProgress = false;

    private void enqueueGattOperation(Runnable operation) {
        Log.d(TAG, "Enqueuing GATT operation");
        gattOperationQueue.add(operation);
        if (!isGattOperationInProgress) {
            executeNextGattOperation();
        }
    }

    private void executeNextGattOperation() {
        if (gattOperationQueue.isEmpty()) {
            Log.d(TAG, "No more GATT operations to execute");
            isGattOperationInProgress = false;
            return;
        }
        Log.d(TAG, "Executing next GATT operation");
        isGattOperationInProgress = true;
        Runnable operation = gattOperationQueue.poll();
        if (operation != null)
        {
            operation.run();
        }
    }

    private void onGattOperationCompleted() {
        Log.d(TAG, "Gatt operation completed");
        isGattOperationInProgress = false;
        executeNextGattOperation();
    }
    private void handlePkocTimeout() {
        Log.e(TAG, "PKOC transaction failed due to timeout");
        // Notify higher layers or take appropriate action
        // For example, you might want to close the connection or retry the transaction
        // You can also update the UI or log the error as needed
    }

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = () ->
    {
        Log.e(TAG, "PKOC transaction timed out");
        // Handle the timeout (e.g., notify higher layers, close connection)
        handlePkocTimeout();
    };

    private boolean isValidPublicKey(byte[] publicKey, byte[] transactionId, byte[] signature) {
        try {
            ECDomainParameters ecParams = CryptoProvider.getDomainParameters();
            ECPoint ecPoint = ecParams.getCurve().decodePoint(publicKey);
            ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(ecPoint, ecParams);
            ECDSASigner signer = new ECDSASigner();
            signer.init(false, pubKeyParams);
            BigInteger r = new BigInteger(1, Arrays.copyOfRange(signature, 0, 32));
            BigInteger s = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));
            byte[] hash = CryptoProvider.getSHA256(transactionId);
            return signer.verifySignature(hash, r, s);
        } catch (Exception e) {
            Log.e(TAG, "Invalid public key", e);
            return false;
        }
    }
    private void showInvalidKeyDialog() {
        runOnUiThread(() -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Invalid Key Validation")
                    .setMessage("The public key is invalid. Please try again.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // Dismiss the dialog
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }
    private byte[] extractPublicKey(byte[] authResponse) {
        int index = 0;
        while (index < authResponse.length) {
            byte tag = authResponse[index];
            int length = authResponse[index + 1];
            if (tag == (byte) 0x5A) { // Public Key tag
                return Arrays.copyOfRange(authResponse, index + 2, index + 2 + length);
            }
            index += 2 + length;
        }
        return null;
    }

    private byte[] extractSignature(byte[] authResponse) {
        int index = 0;
        while (index < authResponse.length) {
            byte tag = authResponse[index];
            int length = authResponse[index + 1];
            if (tag == (byte) 0x9E) { // Signature tag
                return Arrays.copyOfRange(authResponse, index + 2, index + 2 + length);
            }
            index += 2 + length;
        }
        return null;
    }

}
