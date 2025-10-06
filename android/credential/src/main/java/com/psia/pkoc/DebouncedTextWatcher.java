package com.psia.pkoc;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class DebouncedTextWatcher implements TextWatcher
{
    public interface OnDebouncedChange
    {
        void onDebounced(@NonNull String text);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<EditText, Runnable> PENDING = new HashMap<>();

    private final EditText editText;
    private final long delayMs;
    private final OnDebouncedChange callback;

    private DebouncedTextWatcher(EditText editText, long delayMs, OnDebouncedChange callback)
    {
        this.editText = editText;
        this.delayMs = delayMs;
        this.callback = callback;
    }

    public static void attach(EditText editText, long delayMs, OnDebouncedChange cb)
    {
        DebouncedTextWatcher w = new DebouncedTextWatcher(editText, delayMs, cb);
        editText.addTextChangedListener(w);
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s)
    {
        Runnable prev = PENDING.remove(editText);
        if (prev != null) MAIN.removeCallbacks(prev);

        Runnable task = () -> callback.onDebounced(s.toString());
        PENDING.put(editText, task);
        MAIN.postDelayed(task, delayMs);
    }
}
