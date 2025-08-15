package com.psia.pkoc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.psia.pkoc.databinding.FragmentSettingsBinding;
import java.util.UUID;

public class SettingsFragment extends Fragment
{
    private FragmentSettingsBinding binding;
    private SharedPreferences sharedPrefs;

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void configureListeners()
    {
        binding.TransmissionRadioGroup.setOnCheckedChangeListener((group, checkedId) ->
        {
            if (checkedId == binding.NfcButton.getId())
            {
                binding.BleSettings.setVisibility(View.GONE);
                sharedPrefs
                    .edit()
                    .putInt(PKOC_Preferences.PKOC_TransmissionType, PKOC_TransmissionType.NFC.ordinal())
                    .apply();
            }
            else
            {
                binding.BleSettings.setVisibility(View.VISIBLE);
                sharedPrefs
                    .edit()
                    .putInt(PKOC_Preferences.PKOC_TransmissionType, PKOC_TransmissionType.BLE.ordinal())
                    .apply();
            }
        });

        binding.RadioGroup.setOnCheckedChangeListener((group, checkedId) ->
        {
            if (checkedId == binding.UncompressedButton.getId())
            {

                binding.siteIdentifierLabel.setVisibility(View.GONE);
                binding.siteIdentifierInput.setVisibility(View.GONE);
                binding.readerIdentifierLabel.setVisibility(View.GONE);
                binding.readerIdentifierInput.setVisibility(View.GONE);
                binding.sitePublicKeyLabel.setVisibility(View.GONE);
                binding.sitePublicKeyInput.setVisibility(View.GONE);

                sharedPrefs
                    .edit()
                    .putInt(PKOC_Preferences.PKOC_TransmissionFlow, PKOC_ConnectionType.Uncompressed.ordinal())
                    .apply();
            }
            else
            {

                binding.siteIdentifierLabel.setVisibility(View.VISIBLE);
                binding.siteIdentifierInput.setVisibility(View.VISIBLE);
                binding.readerIdentifierLabel.setVisibility(View.VISIBLE);
                binding.readerIdentifierInput.setVisibility(View.VISIBLE);
                binding.sitePublicKeyLabel.setVisibility(View.VISIBLE);
                binding.sitePublicKeyInput.setVisibility(View.VISIBLE);

                sharedPrefs
                    .edit()
                    .putInt(PKOC_Preferences.PKOC_TransmissionFlow, PKOC_ConnectionType.ECHDE_Full.ordinal())
                    .apply();
            }
        });

        binding.autoDiscoverSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            sharedPrefs
                .edit()
                .putBoolean(PKOC_Preferences.AutoDiscoverDevices, isChecked)
                .apply());

        binding.enableRangingSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            sharedPrefs
                .edit()
                .putBoolean(PKOC_Preferences.EnableRanging, isChecked)
                .apply();

            if (isChecked)
            {
                binding.rangingSliderLabel.setVisibility(View.VISIBLE);
                binding.rangingSlider.setVisibility(View.VISIBLE);
                binding.rangingSliderLabelNear.setVisibility(View.VISIBLE);
                binding.rangingSliderLabelFar.setVisibility(View.VISIBLE);

                SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
                int seekBarProgress = sharedPref.getInt(PKOC_Preferences.RangeValue, 0);
                binding.rangingSlider.setProgress(seekBarProgress);
            }
            else
            {
                binding.rangingSliderLabel.setVisibility(View.GONE);
                binding.rangingSlider.setVisibility(View.GONE);
                binding.rangingSliderLabelNear.setVisibility(View.GONE);
                binding.rangingSliderLabelFar.setVisibility(View.GONE);
            }
        });





        binding.sitePublicKeyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                prefs.edit().putString("PKOC_SiteEphemeralKey", s.toString()).apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        binding.siteIdentifierInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                prefs.edit().putString("PKOC_Site_ID", s.toString()).apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        binding.readerIdentifierInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                prefs.edit().putString("PKOC_Reader_ID", s.toString()).apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

       binding.rangingSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                sharedPrefs
                    .edit()
                    .putInt(PKOC_Preferences.RangeValue, progress)
                    .apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.displayMacSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            sharedPrefs
                .edit()
                .putBoolean(PKOC_Preferences.DisplayMAC, isChecked)
                .apply());
    }

    private void initializeComponents()
    {
        int transmissionTypeInt = sharedPrefs.getInt(PKOC_Preferences.PKOC_TransmissionType, PKOC_TransmissionType.BLE.ordinal());
        PKOC_TransmissionType transmissionType = PKOC_TransmissionType.values()[transmissionTypeInt];

        if (transmissionType == PKOC_TransmissionType.NFC)
        {
            binding.TransmissionRadioGroup.check(binding.NfcButton.getId());
            binding.BleSettings.setVisibility(View.GONE);
        }
        else
        {
            binding.TransmissionRadioGroup.check(binding.BleButton.getId());
            binding.BleSettings.setVisibility(View.VISIBLE);
        }

        int ToFlow_int = sharedPrefs.getInt(PKOC_Preferences.PKOC_TransmissionFlow, PKOC_ConnectionType.Uncompressed.ordinal());
        PKOC_ConnectionType toFlow = PKOC_ConnectionType.values()[ToFlow_int];

        if (toFlow == PKOC_ConnectionType.Uncompressed)
            binding.RadioGroup.check(binding.UncompressedButton.getId());

        if (toFlow == PKOC_ConnectionType.ECHDE_Full)
            binding.RadioGroup.check(binding.ECHDEComplete.getId());

        // Show/hide ECDHE-specific fields
        boolean isEcdhe = toFlow == PKOC_ConnectionType.ECHDE_Full;
        int visibility = isEcdhe ? View.VISIBLE : View.GONE;

        binding.siteIdentifierLabel.setVisibility(visibility);
        binding.siteIdentifierInput.setVisibility(visibility);
        binding.readerIdentifierLabel.setVisibility(visibility);
        binding.readerIdentifierInput.setVisibility(visibility);
        binding.sitePublicKeyLabel.setVisibility(visibility);
        binding.sitePublicKeyInput.setVisibility(visibility);

        boolean AutoDiscover = sharedPrefs.getBoolean(PKOC_Preferences.AutoDiscoverDevices, false);
        binding.autoDiscoverSwitch.setChecked(AutoDiscover);

        boolean enableRanging = sharedPrefs.getBoolean(PKOC_Preferences.EnableRanging, false);
        binding.enableRangingSwitch.setChecked(enableRanging);

        boolean displayMAC = sharedPrefs.getBoolean(PKOC_Preferences.DisplayMAC, true);
        binding.displayMacSwitch.setChecked(displayMAC);

        if(enableRanging)
        {
            binding.rangingSliderLabel.setVisibility(View.VISIBLE);
            binding.rangingSlider.setVisibility(View.VISIBLE);
            binding.rangingSliderLabelNear.setVisibility(View.VISIBLE);
            binding.rangingSliderLabelFar.setVisibility(View.VISIBLE);

            int seekBarProgress = sharedPrefs.getInt(PKOC_Preferences.RangeValue, 0);
            binding.rangingSlider.setProgress(seekBarProgress);
        }
        else
        {
            binding.rangingSliderLabel.setVisibility(View.GONE);
            binding.rangingSlider.setVisibility(View.GONE);
            binding.rangingSliderLabelNear.setVisibility(View.GONE);
            binding.rangingSliderLabelFar.setVisibility(View.GONE);
        }

        if (toFlow == PKOC_ConnectionType.ECHDE_Full) {
            //String siteId = binding.siteIdentifierInput.getText().toString().trim();
            //String readerId = binding.readerIdentifierInput.getText().toString().trim();
            //String siteEphemeralKey = binding.siteEphemeralKeyInput.getText().toString().trim();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String savedEphemeralKey = prefs.getString("PKOC_SiteEphemeralKey", "");
            String savedSiteId = prefs.getString("PKOC_Site_ID", "");
            String savedReaderId = prefs.getString("PKOC_Reader_ID", "");
            binding.sitePublicKeyInput.setText(savedEphemeralKey);
            binding.siteIdentifierInput.setText(savedSiteId);
            binding.readerIdentifierInput.setText(savedReaderId);

            sharedPrefs.edit()
                    .putString("PKOC_SiteEphemeralKey", savedEphemeralKey)
                    .putString("PKOC_Site_ID", savedSiteId)
                    .putString("PKOC_Reader_ID", savedReaderId)
                    .apply();

/*            try {
                UUID readerUUID = UUID.fromString(readerId);
                UUID siteUUID = UUID.fromString(siteId);

                byte[] readerIdentifierBytes = TLVProvider.getByteArrayFromGuid(readerUUID);
                byte[] siteIdentifierBytes = TLVProvider.getByteArrayFromGuid(siteUUID);

                ReaderModel newReader = new ReaderModel(readerIdentifierBytes, siteIdentifierBytes);

                if (!Constants.KnownReaders.contains(newReader)) {
                    Constants.KnownReaders.add(newReader);
                }
            } catch (IllegalArgumentException e) {
                Log.e("SettingsFragment", "Invalid UUID format for Site or Reader Identifier", e);
                // Optionally notify the user via Toast or Snackbar
            }*/
        }
    }



    @Override
    public void onViewCreated (@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        initializeComponents();
        configureListeners();
    }

    @Override
    public void onDestroyView ()
    {
        super.onDestroyView();
        binding = null;
    }
}