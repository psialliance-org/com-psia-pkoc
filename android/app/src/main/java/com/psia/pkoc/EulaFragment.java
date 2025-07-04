package com.psia.pkoc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import com.psia.pkoc.databinding.FragmentEulaBinding;

public class EulaFragment extends Fragment
{
    public EulaFragment ()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        com.psia.pkoc.databinding.FragmentEulaBinding binding = FragmentEulaBinding.inflate(inflater, container, false);
        binding.eulaConfirm.setOnClickListener(v -> AcceptEula());
        binding.eulaCancel.setOnClickListener(v -> DeclineEula());

        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(false);

        WebView webView = binding.eulaContent;

        try
        {
            InputStream inputStream = getResources().openRawResource(R.raw.eula);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null)
            {
                stringBuilder.append(line).append("\n");
            }

            webView.loadDataWithBaseURL(null, stringBuilder.toString(), "text/html", "UTF-8", null);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return binding.getRoot();
    }

    private void AcceptEula()
    {
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putBoolean(PKOC_Preferences.EulaAccepted, true);

        editor.apply();

        NavController navController2 = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navController2.navigate(R.id.sendCredentialFragment);

    }

    private void DeclineEula()
    {
        requireActivity().finishAffinity();
    }
}