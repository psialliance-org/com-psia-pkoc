package com.psia.pkoc;

import android.content.Context;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SharedPreferences wrapper for persisting selected credential hex IDs.
 */
public class CredentialStore
{
    private static final String PREFS_NAME = "pkoc_credential_store";
    private static final String KEY_CREDENTIAL_IDS = "selected_credential_ids";

    private CredentialStore() {}

    public static void saveSelectedCredentials(Context context, List<String> credentialHexIds)
    {
        String joined = String.join(",", credentialHexIds);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CREDENTIAL_IDS, joined)
                .apply();
    }

    public static Set<String> getSelectedCredentialIds(Context context)
    {
        String stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CREDENTIAL_IDS, "");
        if (stored.isEmpty())
        {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<>();
        for (String id : stored.split(","))
        {
            String trimmed = id.trim();
            if (!trimmed.isEmpty())
            {
                ids.add(trimmed);
            }
        }
        return ids;
    }

    public static void clear(Context context)
    {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CREDENTIAL_IDS)
                .apply();
    }
}
