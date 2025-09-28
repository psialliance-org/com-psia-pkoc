package com.psia.pkoc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.psia.pkoc.core.PKOC_ConnectionType;
import com.psia.pkoc.core.PKOC_Preferences;
import com.psia.pkoc.core.PKOC_TransmissionType;
import com.psia.pkoc.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment
{
    private FragmentSettingsBinding binding;
    private SharedPreferences sharedPrefs;

    private void persistSiteIfValid()
    {
        String siteUuidStr = safeText(binding.siteIdentifierInput);
        String pubKeyHex   = safeText(binding.sitePublicKeyInput);

        boolean ok = true;

        if (Validators.isValidUuid(siteUuidStr))
        {
            setError(binding.siteIdentifierInput, "Invalid Site UUID");
            ok = false;
        }
        else
        {
            clearError(binding.siteIdentifierInput);
        }

        if (!Validators.isValidHex(pubKeyHex, 65))
        {
            setError(binding.sitePublicKeyInput, "Must be 65-byte hex (130 chars)");
            ok = false;
        }
        else
        {
            clearError(binding.sitePublicKeyInput);
        }

        if (!ok) return;

        java.util.UUID siteUuid = java.util.UUID.fromString(siteUuidStr);
        byte[] sid = UuidConverters.fromUuid(siteUuid);
        byte[] pk  = org.bouncycastle.util.encoders.Hex.decode(pubKeyHex);

        PKOC_Application.getDb().getQueryExecutor().execute(() ->
            PKOC_Application.getDb().siteDao().upsert(new SiteModel(sid, pk)));
    }

    private void persistReaderIfValid()
    {
        String readerUuidStr = safeText(binding.readerIdentifierInput);
        String siteUuidStr   = safeText(binding.siteIdentifierInput);

        boolean ok = true;

        if (Validators.isValidUuid(readerUuidStr))
        {
            setError(binding.readerIdentifierInput, "Invalid Reader UUID");
            ok = false;
        }
        else
        {
            clearError(binding.readerIdentifierInput);
        }

        if (Validators.isValidUuid(siteUuidStr))
        {
            setError(binding.siteIdentifierInput, "Invalid Site UUID");
            ok = false;
        }
        else
        {
            clearError(binding.siteIdentifierInput);
        }

        if (!ok) return;

        byte[] rid = UuidConverters.fromUuid(java.util.UUID.fromString(readerUuidStr));
        byte[] sid = UuidConverters.fromUuid(java.util.UUID.fromString(siteUuidStr));

        PKOC_Application.getDb().getQueryExecutor().execute(() ->
            PKOC_Application.getDb().readerDao().upsert(new ReaderModel(rid, sid)));
    }

    // ---- tiny UI helpers ----
    private static String safeText(@NonNull android.widget.TextView tv)
    {
        CharSequence cs = tv.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private static void setError(@NonNull android.widget.TextView tv, @NonNull String msg)
    {
        if (tv.getParent() instanceof com.google.android.material.textfield.TextInputLayout)
        {
            ((com.google.android.material.textfield.TextInputLayout) tv.getParent()).setError(msg);
        }
        else
        {
            tv.setError(msg);
        }
    }

    private static void clearError(@NonNull android.widget.TextView tv)
    {
        if (tv.getParent() instanceof com.google.android.material.textfield.TextInputLayout)
        {
            ((com.google.android.material.textfield.TextInputLayout) tv.getParent()).setError(null);
        }
        else
        {
            tv.setError(null);
        }
    }

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

        DebouncedTextWatcher.attach(binding.siteIdentifierInput, 300, t -> persistSiteIfValid());
        DebouncedTextWatcher.attach(binding.sitePublicKeyInput,  300, t -> persistSiteIfValid());
        DebouncedTextWatcher.attach(binding.readerIdentifierInput, 300, t -> persistReaderIfValid());

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

        if (toFlow == PKOC_ConnectionType.ECHDE_Full)
        {
            String savedEphemeralKey = sharedPrefs.getString("PKOC_SiteEphemeralKey", "");
            String savedSiteId = sharedPrefs.getString("PKOC_Site_ID", "");
            String savedReaderId = sharedPrefs.getString("PKOC_Reader_ID", "");
            binding.sitePublicKeyInput.setText(savedEphemeralKey);
            binding.siteIdentifierInput.setText(savedSiteId);
            binding.readerIdentifierInput.setText(savedReaderId);

            sharedPrefs.edit()
                    .putString("PKOC_SiteEphemeralKey", savedEphemeralKey)
                    .putString("PKOC_Site_ID", savedSiteId)
                    .putString("PKOC_Reader_ID", savedReaderId)
                    .apply();
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