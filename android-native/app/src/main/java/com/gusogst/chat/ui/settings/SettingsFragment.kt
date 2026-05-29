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
import com.gusogst.chat.util.MaterialAnimator

class SettingsFragment : Fragment() {

    data class SettingItem(
        val key: String,
        val icon: String,
        val label: String,
        val desc: String,
        val color: String
    )

    private val settingsItems = listOf(
        SettingItem("basic", "\uD83C\uDFA8", "\u5916\u89C2", "\u4E3B\u9898\u3001\u6BDB\u73BB\u7483\u3001HDR", "#E94560"),
        SettingItem("model", "\uD83E\uDD16", "\u6A21\u578B", "\u53C2\u6570\u3001\u6E29\u5EA6\u3001\u4E0A\u4E0B\u6587", "#6C5CE7"),
        SettingItem("platform", "\uD83D\uDD17", "\u5E73\u53F0", "MCP\u3001Agent\u3001\u5DE5\u5177", "#3498DB"),
        SettingItem("memory", "\uD83E\uDDE0", "\u8BB0\u5FC6", "\u957F\u671F\u8BB0\u5FC6\u3001\u4E0A\u4E0B\u6587\u7BA1\u7406", "#FDCB6E"),
        SettingItem("search", "\uD83D\uDD0D", "\u641C\u7D22", "\u8054\u7F51\u641C\u7D22\u3001\u641C\u7D22\u5F15\u64CE", "#FF9800"),
        SettingItem("about", "\u2139\uFE0F", "\u5173\u4E8E", "\u7248\u672C\u3001\u5F00\u6E90\u8BB8\u53EF", "#8888A0")
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
            elevation = 2f * resources.displayMetrics.density
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
            setTextColor(resources.getColor(R.color.text_primary, null))
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
            text = "\u203A"
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 22f
        }

        card.addView(iconFrame)
        card.addView(textLayout)
        card.addView(arrowTv)

        // 按压缩放
        MaterialAnimator.applyButtonPress(card)

        card.setOnClickListener {
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
