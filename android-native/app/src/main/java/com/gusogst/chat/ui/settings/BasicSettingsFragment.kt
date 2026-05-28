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
import com.gusogst.chat.viewmodel.ChatViewModel

class BasicSettingsFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var root: LinearLayout

    private val themeOptions = listOf(
        Triple("system", "\u7CFB\u7EDF", "\u25C8"),
        Triple("light", "\u6D45\u8272", "\u2600"),
        Triple("dark", "\u6DF1\u8272", "\u263E"),
        Triple("pureWhite", "\u7EAF\u767D", "\u25CB"),
        Triple("pureBlack", "\u7EAF\u9ED1", "\u25CF")
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
        addHeader("\u57FA\u672C\u8BBE\u7F6E")

        // Theme 5-grid
        addSection("\u4E3B\u9898\u6A21\u5F0F", "\u2728") {
            val grid = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
            for ((key, label, icon) in themeOptions) {
                val isActive = key == theme
                grid.addView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    setPadding(dp(4), dp(14), dp(4), dp(14))
                    background = GradientDrawable().apply {
                        setColor(if (isActive) Color.parseColor("#26E94560") else Color.parseColor("#0AFFFFFF"))
                        setStroke(if (isActive) 2 else 1, if (isActive) Color.parseColor("#80E94560") else Color.TRANSPARENT)
                        cornerRadius = dp(14).toFloat()
                    }
                    addView(TextView(requireContext()).apply { text = icon; textSize = 22f; setTextColor(if (isActive) resources.getColor(R.color.accent, null) else resources.getColor(R.color.gray_300, null)); gravity = Gravity.CENTER })
                    addView(TextView(requireContext()).apply { text = label; textSize = 11f; setTextColor(if (isActive) resources.getColor(R.color.accent, null) else resources.getColor(R.color.gray_300, null)); gravity = Gravity.CENTER })
                    setOnClickListener { viewModel.updateSettings { it.copy(theme = key) } }
                }, lp)
            }
            return@addSection grid
        }

        // Font size (navigate to subpage)
        addSection("字号大小", "A") {
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)) }
            row.addView(TextView(requireContext()).apply { text = "A"; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 16f; setPadding(0, 0, dp(12), 0) })
            row.addView(TextView(requireContext()).apply {
                text = "${fontSize}px"; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(requireContext()).apply {
                text = ">"; setTextColor(resources.getColor(R.color.gray_500, null)); textSize = 16f
            })
            row.background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(resources.getColor(R.color.bg_secondary, null)) }
            row.isClickable = true; row.isFocusable = true
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
        addSection("护眼模式", "◉") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(createToggleRow("护眼模式", "降低蓝光，暖色调保护视力", eyeCare) { viewModel.updateSettings { s -> s.copy(eyeCareMode = it) } })
            if (eyeCare) {
                // 暖色强度滑块
                val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, 0) }
                row.addView(TextView(requireContext()).apply { text = "暖度"; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 12f; setPadding(0, 0, dp(8), 0) })
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
                row.addView(TextView(requireContext()).apply { text = "$eyeCareIntensity%"; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 12f; minWidth = dp(30); gravity = Gravity.END })
                col.addView(row)
            }
            return@addSection col
        }

        // Glass
        addSection("玻璃拟态", "●") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(createToggleRow("毛玻璃效果", "背景模糊和透明效果", glass) { viewModel.updateSettings { s -> s.copy(glassEnabled = it) } })
            if (glass) {
                val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, 0) }
                row.addView(TextView(requireContext()).apply { text = "透明度"; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 12f; setPadding(0, 0, dp(8), 0) })
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
                row.addView(TextView(requireContext()).apply { text = "$glassOpacity%"; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 12f; minWidth = dp(30); gravity = Gravity.END })
                col.addView(row)
            }
            return@addSection col
        }

        // Haptic
        addSection("\u89E6\u89C9\u53CD\u9988", "\u25C9") {
            return@addSection createToggleRow("\u9707\u52A8\u53CD\u9988", "\u6309\u94AE\u70B9\u51FB\u65F6\u7684\u7EBF\u6027\u9A6C\u8FBE\u89E6\u611F\u53CD\u9988", haptic) { viewModel.updateSettings { s -> s.copy(hapticEnabled = it) } }
        }

        // HDR
        addSection("HDR \u6E32\u67D3", "\u2600") {
            return@addSection createToggleRow("HDR \u73BB\u7483\u8D28\u611F", "\u5229\u7528\u5C4F\u5E55\u9AD8\u4EAE\u5EA6\u6A21\u62DF\u771F\u5B9E\u73BB\u7483\u900F\u5149\u6548\u679C", hdr) { viewModel.updateSettings { s -> s.copy(hdrEnabled = it) } }
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
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(18))
            background = GradientDrawable().apply { setColor(Color.parseColor("#03FFFFFF")); setStroke(1, Color.parseColor("#05FFFFFF")); cornerRadius = dp(16).toFloat() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(8), dp(16), dp(0)) }
        }
        val header = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(14)) }
        header.addView(TextView(requireContext()).apply { text = icon; textSize = 18f; setPadding(0, 0, dp(8), 0) })
        header.addView(TextView(requireContext()).apply { text = title; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f; setTypeface(null, Typeface.BOLD) })
        card.addView(header); card.addView(content()); root.addView(card)
    }

    private fun createToggleRow(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(10)); setOnClickListener { onChange(!checked) } }
        val textCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        textCol.addView(TextView(requireContext()).apply { text = label; setTextColor(resources.getColor(R.color.gray_100, null)); textSize = 14f })
        textCol.addView(TextView(requireContext()).apply { text = desc; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 12f; setPadding(0, dp(2), 0, 0) })
        row.addView(textCol)
        val toggle = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(26)).apply { marginStart = dp(12) }
            background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(if (checked) resources.getColor(R.color.accent, null) else Color.parseColor("#1AFFFFFF")) }
        }
        toggle.addView(View(requireContext()).apply {
            val lp = FrameLayout.LayoutParams(dp(22), dp(22)); lp.setMargins(dp(if (checked) 22 else 2), dp(2), 0, 0); layoutParams = lp
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resources.getColor(R.color.text_primary, null)) }
        })
        row.addView(toggle)
        return row
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
