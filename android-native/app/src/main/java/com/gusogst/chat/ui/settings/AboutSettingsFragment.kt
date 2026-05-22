package com.gusogst.chat.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gusogst.chat.R

class AboutSettingsFragment : Fragment() {
    private lateinit var root: LinearLayout

    companion object {
        private const val VERSION = "0.1.0-dev"
        private const val BUILD = "2026.05.17"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); buildUI() }

    private fun buildUI() {
        root.removeAllViews()
        addHeader("\u5173\u4E8E")

        // Logo + version card with gradient
        val logoCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(24), dp(32), dp(24), dp(32))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#03FFFFFF"))
                setStroke(1, Color.parseColor("#05FFFFFF"))
                cornerRadius = dp(16).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(12), dp(16), dp(0)) }
        }
        // App icon placeholder
        logoCard.addView(TextView(requireContext()).apply {
            text = "\u2764"; textSize = 48f; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.accent, null))
        })
        logoCard.addView(TextView(requireContext()).apply {
            text = "gusogst"; textSize = 24f; setTypeface(null, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.text_primary, null)); gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
        })
        logoCard.addView(TextView(requireContext()).apply {
            text = "AI Virtual Companion"; textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null)); gravity = Gravity.CENTER
        })
        logoCard.addView(TextView(requireContext()).apply {
            text = "v$VERSION \u00b7 $BUILD"; textSize = 12f
            setTextColor(resources.getColor(R.color.text_tertiary, null)); gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        root.addView(logoCard)

        // Info sections
        addInfoCard("\u6280\u672F\u6808", listOf(
            "\u524D\u7AEF" to "Capacitor + Web",
            "\u5E73\u53F0" to "Android / iOS",
            "AI" to "OpenAI / Anthropic / Ollama",
            "\u641C\u7D22" to "DuckDuckGo / Tavily"
        ))

        addInfoCard("\u5F00\u6E90", listOf(
            "\u4ED3\u5E93" to "github.com/suer781/chat-gusogst",
            "\u8BB8\u53EF\u8BC1" to "MIT License"
        ))

        // Version check button
        val checkBtn = TextView(requireContext()).apply {
            text = "\u68C0\u67E5\u66F4\u65B0"; setTextColor(resources.getColor(R.color.accent, null)); textSize = 14f
            gravity = Gravity.CENTER; setPadding(dp(12), dp(14), dp(12), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT); setStroke(1, resources.getColor(R.color.accent, null)); cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(16), dp(16), dp(0)) }
        }
        root.addView(checkBtn)
    }

    private fun addInfoCard(title: String, items: List<Pair<String, String>>) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { setColor(Color.parseColor("#03FFFFFF")); setStroke(1, Color.parseColor("#05FFFFFF")); cornerRadius = dp(16).toFloat() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(8), dp(16), dp(0)) }
        }
        card.addView(TextView(requireContext()).apply {
            text = title; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f
            setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, dp(10))
        })
        for ((label, value) in items) {
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(4)) }
            row.addView(TextView(requireContext()).apply {
                text = label; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(requireContext()).apply {
                text = value; setTextColor(resources.getColor(R.color.gray_200, null)); textSize = 13f
            })
            card.addView(row)
        }
        root.addView(card)
    }

    private fun addHeader(title: String) {
        root.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(12))
            addView(TextView(requireContext()).apply { text = "\u2190"; setTextColor(resources.getColor(R.color.accent, null)); textSize = 20f; setPadding(dp(4), dp(4), dp(12), dp(4)); setOnClickListener { parentFragmentManager.popBackStack() } })
            addView(TextView(requireContext()).apply { text = title; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 18f; setTypeface(null, Typeface.BOLD) })
        })
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
