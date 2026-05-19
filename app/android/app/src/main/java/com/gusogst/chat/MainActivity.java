package com.gusogst.chat;

import android.content.res.Configuration;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatDelegate;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure app follows system dark/light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Force WebView to re-evaluate prefers-color-scheme on system theme change
        WebView webView = getBridge().getWebView();
        if (webView != null) {
            webView.evaluateJavascript(
                "(function(){var m=window.matchMedia('(prefers-color-scheme:dark)');document.documentElement.setAttribute('data-theme',m.matches?'dark':'light')})();",
                null
            );
        }
    }
}
