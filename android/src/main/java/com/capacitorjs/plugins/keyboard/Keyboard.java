package com.capacitorjs.plugins.keyboard;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.getcapacitor.Logger;

public class Keyboard {

    interface KeyboardEventListener {
        void onKeyboardEvent(String event, int size);
    }

    private AppCompatActivity activity;
    private ViewTreeObserver.OnGlobalLayoutListener list;

    @Nullable
    public KeyboardEventListener getKeyboardEventListener() {
        return keyboardEventListener;
    }

    public void setKeyboardEventListener(@Nullable KeyboardEventListener keyboardEventListener) {
        this.keyboardEventListener = keyboardEventListener;
    }

    @Nullable
    private KeyboardEventListener keyboardEventListener;

    static final String EVENT_KB_WILL_SHOW = "keyboardWillShow";
    static final String EVENT_KB_DID_SHOW = "keyboardDidShow";
    static final String EVENT_KB_WILL_HIDE = "keyboardWillHide";
    static final String EVENT_KB_DID_HIDE = "keyboardDidHide";

    private int previousHeightDiff = 0;

    public Keyboard(AppCompatActivity activity, boolean resizeOnFullScreen) {
        this.activity = activity;

        // calculate density-independent pixels (dp)
        // http://developer.android.com/guide/practices/screens_support.html
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        final float density = dm.density;

        setKeyboardNone();

        new HeightProvider(this.activity).init().setHeightListener(new HeightProvider.HeightListener() {
            @Override
            public void onHeightChanged(int height) {
                int pixelHeightDiff = (int) (height / density);

                if (pixelHeightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                    if (keyboardEventListener != null) {
                        keyboardEventListener.onKeyboardEvent(EVENT_KB_WILL_SHOW, pixelHeightDiff);
                        keyboardEventListener.onKeyboardEvent(EVENT_KB_DID_SHOW, pixelHeightDiff);
                    } else {
                        Logger.warn("Native Keyboard Event Listener not found");
                    }
                } else if ((previousHeightDiff - pixelHeightDiff) > 100) {
                    if (keyboardEventListener != null) {
                        keyboardEventListener.onKeyboardEvent(EVENT_KB_WILL_HIDE, 0);
                        keyboardEventListener.onKeyboardEvent(EVENT_KB_DID_HIDE, 0);
                    } else {
                        Logger.warn("Native Keyboard Event Listener not found");
                    }
                }

                previousHeightDiff = pixelHeightDiff;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private int getLegacyStableInsetBottom(WindowInsets windowInsets) {
        return windowInsets.getStableInsetBottom();
    }

    @SuppressWarnings("deprecation")
    private Point getLegacySizePoint() {
        // calculate screen height differently for android versions <23: Lollipop 5.x,
        // Marshmallow 6.x
        // http://stackoverflow.com/a/29257533/3642890 beware of nexus 5
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public void show() {
        ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(activity.getCurrentFocus(), 0);
    }

    public boolean hide() {
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = activity.getCurrentFocus();
        if (v == null) {
            return false;
        } else {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            return true;
        }
    }

    public void setKeyboardNone() {
        activity.runOnUiThread(() -> {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        });
    }

}
