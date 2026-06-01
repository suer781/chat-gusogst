package com.gusogst.chat.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gusogst.chat.R
import com.gusogst.chat.model.AppConfig
import com.gusogst.chat.model.UserConfig

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_primary_dark))
            setPadding(16.dp(), 0, 16.dp(), 0)
        }

        root.addView(title("设置"))

        // === Settings cards ===
        root.addView(card(
            label = "基本设置",
            sub = "显示模式、字体、亮度",
            onClick = { findNavController().navigate(R.id.action_settings_to_basic) }
        ))
        root.addView(card(
            label = "模型设置",
            sub = "模型配置、API",
            onClick = { findNavController().navigate(R.id.action_settings_to_model) }
        ))
        root.addView(card(
            label = "人格设置",
            sub = "自定义人格",
            onClick = { findNavController().navigate(R.id.action_settings_to_persona) }
        ))
        root.addView(card(
            label = "高级设置",
            sub = "网络、开发者选项",
            onClick = { findNavController().navigate(R.id.action_settings_to_advance) }
        ))
        root.addView(card(
            label = "关于",
            sub = "版本号、开发日志",
            onClick = { findNavController().navigate(R.id.action_settings_to_about) }
        ))

        // Version
        val appVersion = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        root.addView(TextView(requireContext()).apply {
            text = "版本: $appVersion  |  由 WYF 精心打造"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400))
            textSize = 12f
            setPadding(0, 24.dp(), 0, 0)
        })

        // Logout button
        root.addView(TextView(requireContext()).apply {
            text = "\uD83D\uDEAA 退出登录"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
            textSize = 16f
            setPadding(0, 32.dp(), 0, 0)
            setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("退出登录")
                    .setMessage("确定要退出登录吗？")
                    .setPositiveButton("确定") { _, _ ->
                        UserConfig.clearToken()
                        UserConfig.saveLoginState(requireContext(), false)
                        requireActivity().finish()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        })

        return ScrollView(requireContext()).apply { addView(root) }
    }

    override fun onResume() {
        super.onResume()
        updateUserInfo()
    }

    private fun updateUserInfo() {
        // TODO: Implement user info update
    }

    private fun showUpdateDialog(
        context: android.content.Context,
        latestVersion: String,
        description: String,
        downloadUrl: String
    ) {
        val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(description)
        }

        AlertDialog.Builder(context)
            .setTitle("发现新版本 V$latestVersion")
            .setMessage(msg)
            .setPositiveButton("立即更新") { _, _ ->
                openUrl(downloadUrl)
            }
            .setNegativeButton("暂不更新", null)
            .show()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForUpdate(context: android.content.Context, skipCheck: Boolean = false) {
        // TODO: Implement update check logic
    }

    private fun title(text: String) = TextView(requireContext()).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_dark))
        textSize = 24f
        setPadding(0, 24.dp(), 0, 16.dp())
    }

    private fun card(label: String, sub: String, onClick: () -> Unit) = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_secondary_dark))
        setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
        addView(TextView(requireContext()).apply {
            text = label
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_dark))
            textSize = 16f
        })
        addView(TextView(requireContext()).apply {
            text = sub
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400))
            textSize = 12f
        })
        setOnClickListener { onClick() }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = 8.dp()
        layoutParams = lp
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}