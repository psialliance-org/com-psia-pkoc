package com.psia.pkoc;

import static com.psia.pkoc.CryptoProvider.GetPublicKey;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.math.BigInteger;
import java.security.PublicKey;

public class DisplayPKFragment extends Fragment {

    private String formattedKey = "";
    private ImageView qrImageView;
    private TextView publicKeyTextView;
    private Spinner keyOptionSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_displaypk, container, false);
        qrImageView = view.findViewById(R.id.qrImageView);
        publicKeyTextView = view.findViewById(R.id.publicKeyTextView);
        Button copyKeyButton = view.findViewById(R.id.copyKeyButton);
        keyOptionSpinner = view.findViewById(R.id.keyOptionSpinner);

        try {
            PublicKey publicKey = (PublicKey) GetPublicKey();

            if (publicKey != null) {
                byte[] uncompressedKey = CryptoProvider.getUncompressedPublicKeyBytes();
                StringBuilder hexBuilder = new StringBuilder();
                for (byte b : uncompressedKey) {
                    hexBuilder.append(String.format("%02x", b));
                }
                formattedKey = hexBuilder.toString();
                Log.i("FormattedPublicKey", formattedKey);
                publicKeyTextView.setText(formattedKey);
                updateQRCode(getSelectedKeyData());
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.key_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyOptionSpinner.setAdapter(adapter);

        keyOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    updateQRCode(getSelectedKeyData());
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        copyKeyButton.setOnClickListener(v -> {
            String textToCopy = getSelectedKeyData();
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Public Key", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private String getSelectedKeyData() {

        Object selectedItem = keyOptionSpinner.getSelectedItem();
        String selectedOption = selectedItem != null ? selectedItem.toString() : "Full Public Key";

        if (!selectedOption.equals("Full Public Key") && formattedKey.length() >= 66) {
            String keySegment = formattedKey.substring(2, 66); // skip first 2 chars, take next 64
            int length = 0;
            if (selectedOption.equals("64-bit")) {
                length = 16;
            } else if (selectedOption.equals("128-bit")) {
                length = 32;
            } else if (selectedOption.equals("256-bit")) {
                length = 64;
            }
            if (length > 0 && keySegment.length() >= length) {
                String hexPart = keySegment.substring(keySegment.length() - length);
                BigInteger decimalValue = new BigInteger(hexPart, 16);
                return decimalValue.toString();
            }
        }
        return formattedKey;
    }

    private void updateQRCode(String data) throws WriterException {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 600, 600);
        qrImageView.setImageBitmap(bitmap);
    }
}