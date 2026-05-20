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

        // Enable HDR wide color gamut
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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
