package com.gusogst.chat;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatDelegate;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Force hardware acceleration for backdrop-filter / HDR
        try {
            getBridge().getWebView().setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        } catch (Exception ignored) {}

        // Enable HDR wide color gamut
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setColorMode(ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT);
            getWindow().setFormat(PixelFormat.RGBA_F16);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        WebView webView = getBridge().getWebView();
        if (webView != null) {
            webView.evaluateJavascript(
                "(function(){var m=window.matchMedia('(prefers-color-scheme:dark)');document.documentElement.setAttribute('data-theme',m.matches?'dark':'light')})();",
                null
            );
        }
    }
}
