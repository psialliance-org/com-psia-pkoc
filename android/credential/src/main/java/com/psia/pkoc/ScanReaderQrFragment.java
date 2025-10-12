package com.psia.pkoc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.psia.pkoc.databinding.FragmentScanReaderQrBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class ScanReaderQrFragment extends Fragment
{
    private FragmentScanReaderQrBinding binding;
    private ScanReaderQrViewModel viewModel;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->
            {
                if (isGranted)
                {
                    startCamera();
                }
                else
                {
                    Toast.makeText(requireContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this).popBackStack();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        binding = FragmentScanReaderQrBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ScanReaderQrViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        {
            startCamera();
        }
        else
        {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void startCamera()
    {
        binding.barcodeScanner.decodeContinuous(result ->
        {
            if (result.getText() != null)
            {
                handleQrCode(result.getText());
            }
        });
    }

    private void handleQrCode(String contents)
    {
        try
        {
            JSONObject jsonObject = new JSONObject(contents);
            String siteUuid = jsonObject.getString("siteUuid");
            String readerUuid = jsonObject.getString("readerUuid");
            String publicKey = jsonObject.getString("publicKey");

            viewModel.upsertReader(siteUuid, readerUuid, publicKey);
            NavHostFragment.findNavController(this).popBackStack();
        }
        catch (JSONException e)
        {
            Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        binding.barcodeScanner.resume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        binding.barcodeScanner.pause();
    }
}