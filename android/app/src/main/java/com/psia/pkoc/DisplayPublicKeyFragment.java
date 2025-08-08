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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.psia.pkoc.databinding.FragmentDisplayPublicKeyBinding;

import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.PublicKey;

public class DisplayPublicKeyFragment extends Fragment {
    private String formattedKey = "";
    private FragmentDisplayPublicKeyBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDisplayPublicKeyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            PublicKey publicKey = (PublicKey) GetPublicKey();

            if (publicKey != null) {
                byte[] uncompressedKey = CryptoProvider.getUncompressedPublicKeyBytes();
                formattedKey = Hex.toHexString(uncompressedKey);
                Log.i("Formatted Public Key", formattedKey);
                binding.publicKeyTextView.setText(formattedKey);
                updateQRCode(getSelectedKeyData());
            }
        } catch (WriterException e) {
            Log.e("MainActivity", "An exception was hit in DisplayPublicKeyFragment.onViewCreated: " + e.getMessage());
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.key_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.keyOptionSpinner.setAdapter(adapter);

        binding.keyOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    binding.publicKeyTextView.setText(getSelectedKeyData());
                    updateQRCode(getSelectedKeyData());
                } catch (WriterException e) {
                    Log.e("MainActivity", "An exception was hit in DisplayPublicKeyFragment.onViewCreated: " + e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

       binding.copyKeyButton.setOnClickListener(v -> {
            String textToCopy = getSelectedKeyData();
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Public Key", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });
    }

    private String getSelectedKeyData() {
        Object selectedItem = binding.keyOptionSpinner.getSelectedItem();
        String selectedOption = selectedItem != null ? selectedItem.toString() : "Full Public Key";

        if (!selectedOption.equals("Full Public Key") && formattedKey.length() >= 66) {
            String keySegment = formattedKey.substring(2, 66); // skip first 2 chars, take next 64
            int length = 0;
            switch (selectedOption)
            {
                case "64-bit":
                    length = 16;
                    break;
                case "128-bit":
                    length = 32;
                    break;
                case "256-bit":
                    length = 64;
                    break;
            }
            if (length > 0)
            {
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
        binding.qrImageView.setImageBitmap(bitmap);
    }
}