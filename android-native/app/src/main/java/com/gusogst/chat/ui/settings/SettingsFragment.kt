package com.gusogst.chat.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.gusogst.chat.R

class SettingsFragment : Fragment() {

    data class SettingItem(
        val key: String,
        val icon: String,
        val label: String,
        val desc: String,
        val color: String
    )

    private val settingsItems = listOf(
        SettingItem("basic", "🎨", "外观", "主题、毛玻璃、HDR", "#E94560"),
        SettingItem("model", "🤖", "模型", "参数、温度、上下文", "#6C5CE7"),
        SettingItem("platform", "🔗", "平台", "MCP、Agent、工具", "#3498DB"),
        SettingItem("memory", "🧠", "记忆", "长期记忆、上下文管理", "#FDCB6E"),
        SettingItem("search", "🔍", "搜索", "联网搜索、搜索引擎", "#FF9800"),
        SettingItem("about", "ℹ️", "关于", "版本、开源许可", "#8888A0")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settingsList = view.findViewById<LinearLayout>(R.id.settingsList)

        for ((index, item) in settingsItems.withIndex()) {
            settingsList.addView(createSettingCard(item, index))
        }
    }

    private fun createSettingCard(item: SettingItem, index: Int): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            val bg = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(resources.getColor(R.color.bg_secondary, null))
                setStroke(1, resources.getColor(R.color.bg_tertiary, null))
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            isClickable = true
            isFocusable = true
        }

        // Icon container
        val iconBg = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(resources.getColor(R.color.bg_tertiary, null))
        }
        val iconTv = TextView(requireContext()).apply {
            text = item.icon
            textSize = 20f
            gravity = Gravity.CENTER
        }
        val iconFrame = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            background = iconBg
            addView(iconTv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        // Text info
        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            }
        }
        textLayout.addView(TextView(requireContext()).apply {
            text = item.label
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        })
        textLayout.addView(TextView(requireContext()).apply {
            text = item.desc
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })

        // Arrow
        val arrowTv = TextView(requireContext()).apply {
            text = "›"
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 22f
        }

        card.addView(iconFrame)
        card.addView(textLayout)
        card.addView(arrowTv)

        card.setOnClickListener {
            // Navigate to sub-settings
            val fragment = when (item.key) {
                "basic" -> BasicSettingsFragment()
                "model" -> ModelSettingsFragment()
                "platform" -> PlatformSettingsFragment()
                "memory" -> MemorySettingsFragment()
                "search" -> SearchSettingsFragment()
                "about" -> AboutSettingsFragment()
                else -> null
            }
            fragment?.let {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, it)
                    .addToBackStack(null)
                    .commit()
            }
        }

        return card
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}