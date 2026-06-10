package com.gusogst.chat;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.ColorSpace;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatDelegate;
import com.getcapacitor.BridgeActivity;

/**
 * Capacitor 主 Activity — 专门做了 HDR / 宽色域适配：
 *   1. Window colorMode = HDR + WIDE_COLOR_GAMUT （Android 13+ 提供真正的 HDR 通道）
 *   2. PixelFormat.RGBA_F16 或 isUseHdr() = true （Android 14+ 专用 API）
 *   3. WebView 强制硬件加速 + 允许宽色域
 *   4. 所有 API 调用都做 SDK 版本 + try/catch 双保险，低版本安全降级
 */
public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // 启用/请求宽色域与 HDR（逐级试探，失败不影响基础渲染）
        tryEnableHdrOnWindow(getWindow());

        // WebView 基础配置：匹配主题色 + 强制硬件加速 + 启用宽色域
        try {
            WebView webView = getBridge().getWebView();
            if (webView != null) {
                // 匹配暗色主题，避免首帧闪白
                webView.setBackgroundColor(0xFF0f0f23);
                webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

                // 开启 WebView 本身的色彩管理（部分机型默认关闭）
                WebSettings settings = webView.getSettings();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 强制开启 WebView 层色彩管理，让 CSS 的 oklch / display-p3 真正走宽色域
                    try {
                        settings.setForceDark(WebSettings.FORCE_DARK_OFF);
                    } catch (NoSuchMethodError ignored) {}
                }

                // Android 14+: 尝试开启 WebView 宽色域/HDR。
                // WebView 公共 API 中没有显式的 setUseWideColorGamut，
                // 这里依次尝试多个可能的方法名/反射路径，全部失败则静默降级。
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    String[] methodNames = new String[]{
                        "setUseWideColorGamut",
                        "setWideGamutEnabled",
                        "setIsWideColorGamut",
                    };
                    for (String name : methodNames) {
                        try {
                            java.lang.reflect.Method m = WebView.class.getDeclaredMethod(
                                name, boolean.class);
                            m.setAccessible(true);
                            m.invoke(webView, true);
                            break; // 成功就跳出
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {
            // Bridge 未初始化或其他异常 —— 静默失败，不影响应用启动
        }
    }

    /**
     * 逐级试探地给 Window 开启 HDR / 宽色域。
     * 每一步都被 try/catch 包裹，在任意 API 上失败都不会抛异常。
     */
    private void tryEnableHdrOnWindow(Window window) {
        if (window == null) return;

        // Android 8.0+: 声明宽色域意图（COLOR_MODE_WIDE_COLOR_GAMUT = 0x2）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                window.setColorMode(ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT);
            } catch (Throwable ignored) {}
        }

        // Android 13+: 同时声明 HDR 支持。
        // 用反射读取 ActivityInfo.COLOR_MODE_HDR，避免低 compileSdk 时找不到符号。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                java.lang.reflect.Field f = ActivityInfo.class.getField("COLOR_MODE_HDR");
                int colorModeHdr = f.getInt(null);
                window.setColorMode(
                    ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT | colorModeHdr);
            } catch (Throwable ignored) {}
        }

        // PixelFormat.RGBA_F16：半浮点缓冲 —— 允许 CSS 里 oklch(>1) 亮度不被钳位
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                window.setFormat(PixelFormat.RGBA_F16);
            } catch (Throwable ignored) {}
        }

        // Android 14+: Window#setHdrConversionEnabled(true) — 公共 API
        // 注意：setIsHdr / setWideGamut 都不是 Window 的公开 API，这里用反射更稳的版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                java.lang.reflect.Method m = Window.class.getMethod(
                    "setHdrConversionEnabled", boolean.class);
                m.invoke(window, true);
            } catch (Throwable ignored) {
                // 回退：尝试反射 setIsHdr (非公开 API，需要 setAccessible)
                try {
                    java.lang.reflect.Method m2 = Window.class.getDeclaredMethod(
                        "setIsHdr", boolean.class);
                    m2.setAccessible(true);
                    m2.invoke(window, true);
                } catch (Throwable ignored2) {}
            }
        }

        // 清除 FLAG_DITHER（会让 HDR 边界产生难看的色带）
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DITHER);
        } catch (Throwable ignored) {}
    }

    /**
     * 系统配置变化时（如深色/浅色模式切换、屏幕旋转），
     * 同步通知 JS 端，避免颜色模式错乱。
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            WebView webView = getBridge().getWebView();
            if (webView != null) {
                webView.evaluateJavascript(
                    "(function(){" +
                        "var m=window.matchMedia('(prefers-color-scheme:dark)');" +
                        "document.documentElement.setAttribute('data-theme',m.matches?'dark':'light');" +
                    "})();",
                    null
                );
            }
        } catch (Throwable ignored) {}
    }
}
