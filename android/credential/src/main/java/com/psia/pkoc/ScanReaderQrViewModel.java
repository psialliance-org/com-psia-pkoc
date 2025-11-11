package com.psia.pkoc;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.bouncycastle.util.encoders.Hex;

import java.util.UUID;

public class ScanReaderQrViewModel extends AndroidViewModel
{
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public ScanReaderQrViewModel(@NonNull Application application)
    {
        super(application);
    }

    public LiveData<String> getToastMessage()
    {
        return toastMessage;
    }

    public void upsertReader(String siteUuid, String readerUuid, String publicKeyHex)
    {
        PKOC_Application.getDb().getQueryExecutor().execute(() ->
        {
            try
            {
                byte[] siteId = UuidConverters.fromUuid(UUID.fromString(siteUuid));
                byte[] readerId = UuidConverters.fromUuid(UUID.fromString(readerUuid));
                byte[] publicKey = Hex.decode(publicKeyHex);

                SiteModel s = PKOC_Application.getDb().siteDao().findById(siteId);
                if (s == null)
                {
                    s = new SiteModel(siteId, publicKey);
                    PKOC_Application.getDb().siteDao().upsert(s);
                }

                ReaderModel reader = PKOC_Application.getDb().readerDao().findByIds(readerId, siteId);
                if (reader == null)
                {
                    reader = new ReaderModel(readerId, siteId);
                    PKOC_Application.getDb().readerDao().upsert(reader);
                    toastMessage.postValue("New reader added");
                }
                else
                {
                    toastMessage.postValue("Reader already exists");
                }
            }
            catch (Exception e)
            {
                toastMessage.postValue("Error processing QR code");
            }
        });
    }
}