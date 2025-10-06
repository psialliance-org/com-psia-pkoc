package com.psia.pkoc.core;

import static com.psia.pkoc.core.CryptoProvider.GetPublicKey;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.psia.pkoc.core.databinding.FragmentDisplayPublicKeyBinding;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.PublicKey;

public class DisplayPublicKeyFragment extends Fragment
{
    private String formattedKey = "";
    private FragmentDisplayPublicKeyBinding binding;

    private boolean initializeForReaderMode()
    {
        SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);

        String readerUUID = prefs.getString(PKOC_Preferences.ReaderUUID, null);
        String siteUUID = prefs.getString(PKOC_Preferences.SiteUUID, null);

        if (readerUUID != null && siteUUID != null)
        {
            binding.keyOptionSpinner.setVisibility(View.INVISIBLE);
            binding.customKeyLengthInput.setVisibility(View.INVISIBLE);
            binding.publicKeyTextView.setVisibility(View.INVISIBLE);
            binding.copyKeyButton.setVisibility(View.INVISIBLE);

            var publicKey = CryptoProvider.getUncompressedPublicKeyBytes();

            try
            {
                JSONObject obj = new JSONObject();
                obj.put("siteUuid", siteUUID);
                obj.put("readerUuid", readerUUID);
                obj.put("publicKey", publicKey);
                String jsonBlob = obj.toString();
                updateQRCode(jsonBlob);
            }
            catch (Exception e)
            {
                return false;
            }

            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        binding = FragmentDisplayPublicKeyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        if (initializeForReaderMode())
        {
            return;
        }

        try
        {
            PublicKey publicKey = (PublicKey) GetPublicKey();

            if (publicKey != null)
            {
                byte[] uncompressedKey = CryptoProvider.getUncompressedPublicKeyBytes();
                formattedKey = Hex.toHexString(uncompressedKey);
                Log.i("Formatted Public Key", formattedKey);
                binding.publicKeyTextView.setText(formattedKey);
                updateQRCode(getSelectedKeyData());
            }
        }
        catch (WriterException e)
        {
            Log.e("MainActivity", "An exception was hit in DisplayPublicKeyFragment.onViewCreated: " + e.getMessage());
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.key_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.keyOptionSpinner.setAdapter(adapter);

        binding.keyOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                try
                {
                    binding.publicKeyTextView.setText(getSelectedKeyData());
                    updateQRCode(getSelectedKeyData());
                }
                catch (WriterException e)
                {
                    Log.e("MainActivity", "An exception was hit in DisplayPublicKeyFragment.onViewCreated: " + e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.copyKeyButton.setOnClickListener(v ->
        {
            String textToCopy = getSelectedKeyData();
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Public Key", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });


        binding.customKeyLengthInput.setOnFocusChangeListener((v, hasFocus) ->
        {
            if (!hasFocus)
            {
                validateCustomKeyLength();
                try
                {
                    String updatedKey = getSelectedKeyData();
                    binding.publicKeyTextView.setText(updatedKey);
                    updateQRCode(updatedKey);
                }
                catch (WriterException e)
                {
                    Log.e("MainActivity", "Error updating QR code after custom length input: " + e.getMessage());
                }
            }
        });

        binding.customKeyLengthInput.addTextChangedListener(new android.text.TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                Object selectedItem = binding.keyOptionSpinner.getSelectedItem();
                if (selectedItem != null && selectedItem.toString().equals("Custom bits (between 64 and 256)")) {
                    try {
                        String updatedKey = getSelectedKeyData();
                        binding.publicKeyTextView.setText(updatedKey);
                        updateQRCode(updatedKey);
                    } catch (WriterException e) {
                        Log.e("MainActivity", "Error updating QR code after custom length change: " + e.getMessage());
                    }
                }
            }
        });

    }

    private String getSelectedKeyData()
    {
        Object selectedItem = binding.keyOptionSpinner.getSelectedItem();
        String selectedOption = selectedItem != null ? selectedItem.toString() : "Full Public Key";

        if (!selectedOption.equals("Full Public Key") && formattedKey.length() >= 66)
        {
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
                case "200-bit":
                    length = 50;
                    break;
                case "256-bit":
                    length = 64;
                    break;
                case "Custom bits (between 64 and 256)":
                    length = getCustomLength(); // returns hex length
                    break;
            }

            if (length > 0)
            {
                if (keySegment.length() >= length)
                {
                    String hexPart = keySegment.substring(keySegment.length() - length);
                    BigInteger decimalValue = new BigInteger(hexPart, 16);
                    return decimalValue.toString();
                }
                else
                {
                    Toast.makeText(requireContext(), "Key segment too short for specified length.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        return formattedKey;
    }

    private int getCustomLength()
    {
        String customLengthText = binding.customKeyLengthInput.getText().toString();
        if (customLengthText.isEmpty())
        {
            return 0;
        }
        try
        {
            int customLength = Integer.parseInt(customLengthText);
            if (customLength >= 65 && customLength <= 255)
            {
                return customLength / 4; // Convert bits to hex length
            }
            else
            {
                Toast.makeText(requireContext(), "Please enter a length between 65 and 255.", Toast.LENGTH_SHORT).show();
            }
        }
        catch (NumberFormatException e)
        {
            Toast.makeText(requireContext(), "Invalid number format.", Toast.LENGTH_SHORT).show();
        }
        return 0;
    }

    private void validateCustomKeyLength()
    {
        String customLengthText = binding.customKeyLengthInput.getText().toString();
        if (customLengthText.isEmpty())
        {
            return;
        }
        try
        {
            int customLength = Integer.parseInt(customLengthText);
            if (customLength < 65 || customLength > 255)
            {
                Toast.makeText(requireContext(), "Please enter a length between 65 and 255.", Toast.LENGTH_SHORT).show();
            }
        }
        catch (NumberFormatException e)
        {
            Toast.makeText(requireContext(), "Invalid number format.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateQRCode(String data) throws WriterException
    {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 600, 600);
        binding.qrImageView.setImageBitmap(bitmap);
    }
}
