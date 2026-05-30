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
        addHeader(getString(R.string.about_title))

        // Logo + version card with gradient
        val logoCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(24), dp(32), dp(24), dp(32))
            background = GradientDrawable().apply {
                setColor(resources.getColor(R.color.bg_secondary, null))
                setStroke(1, resources.getColor(R.color.border_color, null))
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
            text = getString(R.string.about_app_desc); textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null)); gravity = Gravity.CENTER
        })
        logoCard.addView(TextView(requireContext()).apply {
            text = "v$VERSION \u00b7 $BUILD"; textSize = 12f
            setTextColor(resources.getColor(R.color.text_tertiary, null)); gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        root.addView(logoCard)

        // Info sections
        addInfoCard(getString(R.string.about_tech_title), listOf(
            getString(R.string.about_tech_frontend) to getString(R.string.about_tech_frontend_val),
            getString(R.string.about_tech_platform) to getString(R.string.about_tech_platform_val),
            getString(R.string.about_tech_ai) to getString(R.string.about_tech_ai_val),
            getString(R.string.about_tech_search) to getString(R.string.about_tech_search_val)
        ))

        addInfoCard(getString(R.string.about_oss_title), listOf(
            getString(R.string.about_oss_repo) to "github.com/suer781/chat-gusogst",
            getString(R.string.about_oss_license) to getString(R.string.about_oss_license_val)
        ))

        // Version check button
        val checkBtn = TextView(requireContext()).apply {
            text = getString(R.string.about_check_update); setTextColor(resources.getColor(R.color.accent, null)); textSize = 14f
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
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_secondary, null)); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(16).toFloat() }
            elevation = 1f * resources.displayMetrics.density
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
