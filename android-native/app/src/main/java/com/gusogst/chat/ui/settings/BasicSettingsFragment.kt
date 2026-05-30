package com.gusogst.chat.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.util.HdrHelper
import com.gusogst.chat.util.MaterialAnimator
import com.gusogst.chat.viewmodel.ChatViewModel

class BasicSettingsFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var root: LinearLayout

    private val themeOptions get() = listOf(
        Triple("system", getString(R.string.basic_theme_system), "\u25C8"),
        Triple("light", getString(R.string.basic_theme_light), "\u2600"),
        Triple("dark", getString(R.string.basic_theme_dark), "\u263E"),
        Triple("pureWhite", getString(R.string.basic_theme_pure_white), "\u25CB"),
        Triple("pureBlack", getString(R.string.basic_theme_pure_black), "\u25CF")
    )

    private val fontSizes = listOf(12, 13, 14, 15, 16, 17, 18, 20, 22)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.settings.observe(viewLifecycleOwner) { s ->
            buildUI(s.theme, s.fontSize, s.eyeCareMode, s.eyeCareIntensity, s.glassEnabled, s.glassOpacity, s.hapticEnabled, s.hdrEnabled)
        }
    }

    private fun buildUI(theme: String, fontSize: String, eyeCare: Boolean, eyeCareIntensity: Int, glass: Boolean, glassOpacity: Int, haptic: Boolean, hdr: Boolean) {
        root.removeAllViews()
        addHeader(getString(R.string.basic_title))

        // Theme 5-grid
        addSection(getString(R.string.basic_theme_mode), "\u2728") {
            val grid = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
            for ((key, label, icon) in themeOptions) {
                val isActive = key == theme
                grid.addView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    setPadding(dp(4), dp(14), dp(4), dp(14))
                    background = GradientDrawable().apply {
                        setColor(if (isActive) resources.getColor(R.color.accent_soft, null) else resources.getColor(R.color.bg_tertiary, null))
                        setStroke(if (isActive) 2 else 1, if (isActive) resources.getColor(R.color.accent_glow, null) else Color.TRANSPARENT)
                        cornerRadius = dp(14).toFloat()
                    }
                    addView(TextView(requireContext()).apply { text = icon; textSize = 22f; setTextColor(if (isActive) resources.getColor(R.color.accent, null) else resources.getColor(R.color.text_secondary, null)); gravity = Gravity.CENTER })
                    addView(TextView(requireContext()).apply { text = label; textSize = 11f; setTextColor(if (isActive) resources.getColor(R.color.accent, null) else resources.getColor(R.color.text_secondary, null)); gravity = Gravity.CENTER })
                    setOnClickListener { viewModel.updateSettings { it.copy(theme = key) } }
                    MaterialAnimator.applyButtonPress(this)
                }, lp)
            }
            return@addSection grid
        }

        // Font size (navigate to subpage)
        addSection(getString(R.string.basic_font_size), "A") {
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)) }
            row.addView(TextView(requireContext()).apply { text = "A"; setTextColor(resources.getColor(R.color.text_tertiary, null)); textSize = 16f; setPadding(0, 0, dp(12), 0) })
            row.addView(TextView(requireContext()).apply {
                text = "${fontSize}px"; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(requireContext()).apply {
                text = ">"; setTextColor(resources.getColor(R.color.text_tertiary, null)); textSize = 16f
            })
            row.background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(resources.getColor(R.color.bg_secondary, null)) }
            row.isClickable = true; row.isFocusable = true
            MaterialAnimator.applyButtonPress(row)
            row.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragmentContainer, FontSizeFragment())
                    .addToBackStack(null)
                    .commit()
            }
            return@addSection row
        }

        // Eye care
        addSection(getString(R.string.basic_eye_care), "◉") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(createToggleRow(getString(R.string.basic_eye_care), getString(R.string.basic_eye_care_desc), eyeCare) { viewModel.updateSettings { s -> s.copy(eyeCareMode = it) } })
            if (eyeCare) {
                // 暖色强度滑块
                val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, 0) }
                row.addView(TextView(requireContext()).apply { text = getString(R.string.basic_eye_warmth);; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 12f; setPadding(0, 0, dp(8), 0) })
                row.addView(SeekBar(requireContext()).apply {
                    max = 100; progress = eyeCareIntensity
                    progressTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
                    thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) viewModel.updateSettings { it.copy(eyeCareIntensity = progress) } }
                        override fun onStartTrackingTouch(sb: SeekBar?) {}; override fun onStopTrackingTouch(sb: SeekBar?) {}
                    })
                })
                row.addView(TextView(requireContext()).apply { text = "$eyeCareIntensity%"; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 12f; minWidth = dp(30); gravity = Gravity.END })
                col.addView(row)
            }
            return@addSection col
        }

        // Glass
        addSection(getString(R.string.basic_glass_title), "●") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(createToggleRow(getString(R.string.basic_glass_desc), getString(R.string.basic_glass_desc2), glass) { viewModel.updateSettings { s -> s.copy(glassEnabled = it) } })
            if (glass) {
                val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, 0) }
                row.addView(TextView(requireContext()).apply { text = getString(R.string.basic_glass_opacity);; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 12f; setPadding(0, 0, dp(8), 0) })
                row.addView(SeekBar(requireContext()).apply {
                    max = 100; progress = glassOpacity
                    progressTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
                    thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) viewModel.updateSettings { it.copy(glassOpacity = progress) } }
                        override fun onStartTrackingTouch(sb: SeekBar?) {}; override fun onStopTrackingTouch(sb: SeekBar?) {}
                    })
                })
                row.addView(TextView(requireContext()).apply { text = "$glassOpacity%"; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 12f; minWidth = dp(30); gravity = Gravity.END })
                col.addView(row)
            }
            return@addSection col
        }

        // Haptic
        addSection(getString(R.string.basic_haptic_title), "\u25C9") {
            return@addSection createToggleRow(getString(R.string.basic_haptic_desc), getString(R.string.basic_haptic_desc2), haptic) { viewModel.updateSettings { s -> s.copy(hapticEnabled = it) } }
        }

        // HDR
        addSection(getString(R.string.basic_hdr_title), "\u2600") {
            return@addSection createToggleRow(getString(R.string.basic_hdr_desc), getString(R.string.basic_hdr_desc2), hdr) { viewModel.updateSettings { s -> s.copy(hdrEnabled = it) } }
        }
    }

    private fun addHeader(title: String) {
        root.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(12))
            addView(TextView(requireContext()).apply { text = "\u2190"; setTextColor(resources.getColor(R.color.accent, null)); textSize = 20f; setPadding(dp(4), dp(4), dp(12), dp(4)); setOnClickListener { parentFragmentManager.popBackStack() } })
            addView(TextView(requireContext()).apply { text = title; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 18f; setTypeface(null, Typeface.BOLD) })
        })
    }

    private fun addSection(title: String, icon: String, content: () -> View) {
        val isGlass = viewModel.settings.value?.glassEnabled == true
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(18))
            background = GradientDrawable().apply {
                setColor(if (isGlass) resources.getColor(R.color.glass_overlay_white, null) else resources.getColor(R.color.bg_secondary, null))
                setStroke(1, if (isGlass) resources.getColor(R.color.glass_stroke_white, null) else resources.getColor(R.color.border_color, null))
                cornerRadius = dp(16).toFloat()
            }
            elevation = 1f * resources.displayMetrics.density
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(8), dp(16), dp(0)) }
        }
        val header = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(14)) }
        header.addView(TextView(requireContext()).apply { text = icon; textSize = 18f; setPadding(0, 0, dp(8), 0) })
        header.addView(TextView(requireContext()).apply { text = title; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f; setTypeface(null, Typeface.BOLD) })
        card.addView(header); card.addView(content()); root.addView(card)
    }

    private fun createToggleRow(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(10)); setOnClickListener { onChange(!checked) } }
        MaterialAnimator.applyButtonPress(row)
        val textCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        textCol.addView(TextView(requireContext()).apply { text = label; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f })
        textCol.addView(TextView(requireContext()).apply { text = desc; setTextColor(resources.getColor(R.color.text_tertiary, null)); textSize = 12f; setPadding(0, dp(2), 0, 0) })
        row.addView(textCol)
        val toggle = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(26)).apply { marginStart = dp(12) }
            background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(if (checked) resources.getColor(R.color.accent, null) else resources.getColor(R.color.bg_tertiary, null)) }
        }
        val knob = View(requireContext()).apply {
            val lp = FrameLayout.LayoutParams(dp(22), dp(22)); lp.setMargins(dp(if (checked) 22 else 2), dp(2), 0, 0); layoutParams = lp
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resources.getColor(R.color.text_primary, null)) }
        }
        toggle.addView(knob)
        row.addView(toggle)

        // Fix 8: Apply HDR toggle glow when toggle is active
        val isHdr = viewModel.settings.value?.hdrEnabled ?: false
        val isDark = viewModel.settings.value?.let { s ->
            when (s.theme) { "dark", "pureBlack" -> true; "light", "pureWhite" -> false; else -> true }
        } ?: true
        if (checked && isHdr) {
            HdrHelper.applyToggleGlow(knob, true, isDark)
        }

        return row
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
