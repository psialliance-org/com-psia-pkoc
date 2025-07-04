package com.psia.pkoc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.psia.pkoc.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment
{
    private FragmentSettingsBinding binding;
//    private PKOC_ConnectionType ToFlow = PKOC_ConnectionType.Uncompressed;
    private PKOC_ConnectionType ToFlow = PKOC_ConnectionType.ECHDE_Full;

    RadioButton UncButton;
    RadioGroup BtnGrp;

    SwitchCompat AutoDiscoverSwitch;
    SwitchCompat EnableRangingSwitch;
    SeekBar RangeSeekBar;

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        UncButton = binding.UncompressedButton;
        BtnGrp = binding.RadioGroup;
        AutoDiscoverSwitch = binding.autoDiscoverSwitch;
        EnableRangingSwitch = binding.enableRangingSwitch;
        RangeSeekBar = binding.rangingSlider;

        EnableRangingSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            if(isChecked)
            {
                binding.rangingSliderLabel.setVisibility(View.VISIBLE);
                RangeSeekBar.setVisibility(View.VISIBLE);
                binding.rangingSliderLabelNear.setVisibility(View.VISIBLE);
                binding.rangingSliderLabelFar.setVisibility(View.VISIBLE);

                SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
                int seekBarProgress = sharedPref.getInt(PKOC_Preferences.RangeValue, 0);
                RangeSeekBar.setProgress(seekBarProgress);

            }
            else
            {
                binding.rangingSliderLabel.setVisibility(View.GONE);
                RangeSeekBar.setVisibility(View.GONE);
                binding.rangingSliderLabelNear.setVisibility(View.GONE);
                binding.rangingSliderLabelFar.setVisibility(View.GONE);
            }
        });

        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);

        int ToFlow_int = sharedPref.getInt(PKOC_Preferences.PKOC_TransmissionFlow, PKOC_ConnectionType.Uncompressed.ordinal());
        ToFlow = PKOC_ConnectionType.values()[ToFlow_int];

        if (ToFlow == PKOC_ConnectionType.Uncompressed)
            BtnGrp.check(UncButton.getId());

        if (ToFlow == PKOC_ConnectionType.ECHDE_Full)
            BtnGrp.check(binding.ECHDEComplete.getId());

        boolean AutoDiscover = sharedPref.getBoolean(PKOC_Preferences.AutoDiscoverDevices, false);
        AutoDiscoverSwitch.setChecked(AutoDiscover);

        boolean enableRanging = sharedPref.getBoolean(PKOC_Preferences.EnableRanging, false);
        EnableRangingSwitch.setChecked(enableRanging);

        boolean displayMAC = sharedPref.getBoolean(PKOC_Preferences.DisplayMAC, true);
        binding.displayMacSwitch.setChecked(displayMAC);

        if(enableRanging)
        {
            binding.rangingSliderLabel.setVisibility(View.VISIBLE);
            RangeSeekBar.setVisibility(View.VISIBLE);
            binding.rangingSliderLabelNear.setVisibility(View.VISIBLE);
            binding.rangingSliderLabelFar.setVisibility(View.VISIBLE);

            int seekBarProgress = sharedPref.getInt(PKOC_Preferences.RangeValue, 0);
            RangeSeekBar.setProgress(seekBarProgress);
        }
        else
        {
            binding.rangingSliderLabel.setVisibility(View.GONE);
            RangeSeekBar.setVisibility(View.GONE);
            binding.rangingSliderLabelNear.setVisibility(View.GONE);
            binding.rangingSliderLabelFar.setVisibility(View.GONE);

        }
    }

    @Override
    public void onDestroyView ()
    {
        if(BtnGrp.getCheckedRadioButtonId() == UncButton.getId())
            ToFlow = PKOC_ConnectionType.Uncompressed;

        if(BtnGrp.getCheckedRadioButtonId() == binding.ECHDEComplete.getId())
            ToFlow = PKOC_ConnectionType.ECHDE_Full;

        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt(PKOC_Preferences.PKOC_TransmissionFlow, ToFlow.ordinal());
        editor.putBoolean(PKOC_Preferences.AutoDiscoverDevices, AutoDiscoverSwitch.isChecked());
        editor.putBoolean(PKOC_Preferences.EnableRanging, EnableRangingSwitch.isChecked());
        editor.putBoolean(PKOC_Preferences.DisplayMAC, binding.displayMacSwitch.isChecked());

        if(EnableRangingSwitch.isChecked())
            editor.putInt(PKOC_Preferences.RangeValue, RangeSeekBar.getProgress());

        editor.apply();

        super.onDestroyView();
        binding = null;
    }
}