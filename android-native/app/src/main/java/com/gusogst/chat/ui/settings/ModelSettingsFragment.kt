package com.gusogst.chat.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.viewmodel.ChatViewModel

class ModelSettingsFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var root: LinearLayout

    private val providers get() = listOf(
        Triple("openai", "OpenAI", "sk-..."),
        Triple("anthropic", "Anthropic", "sk-ant-..."),
        Triple("ollama", "Ollama", ""),
        Triple("custom", getString(R.string.model_custom), getString(R.string.model_api_key))
    )
    private val tokenOptions = listOf(1024, 2048, 4096, 8192, 16384)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.settings.observe(viewLifecycleOwner) { buildUI(it) }
    }

    private fun buildUI(s: com.gusogst.chat.model.UISettings) {
        root.removeAllViews()
        addHeader(getString(R.string.model_title))

        val currentProvider = viewModel.providers.value?.firstOrNull { it.enabled }?.name?.lowercase() ?: "openai"

        // Provider 4-grid - purple accent
        addSection(getString(R.string.model_provider), "\u2699") {
            val grid = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
            for ((id, label, _) in providers) {
                val isActive = id == currentProvider
                grid.addView(TextView(requireContext()).apply {
                    text = label; textSize = 14f; gravity = Gravity.CENTER; setPadding(dp(4), dp(12), dp(4), dp(12))
                    setTextColor(if (isActive) resources.getColor(R.color.purple, null) else resources.getColor(R.color.gray_300, null))
                    setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
                    background = GradientDrawable().apply {
                        setColor(if (isActive) resources.getColor(R.color.purple_soft, null) else resources.getColor(R.color.bg_tertiary, null))
                        setStroke(if (isActive) 2 else 1, if (isActive) resources.getColor(R.color.purple_soft, null) else Color.TRANSPARENT)
                        cornerRadius = dp(10).toFloat()
                    }
                }, lp)
            }
            return@addSection grid
        }

        // Model name
        addSection(getString(R.string.model_name), "") {
            return@addSection createInput("gpt-4o / claude-3-opus / qwen2", "")
        }

        // API Key
        addSection(getString(R.string.model_api_key), "\u26BF") {
            return@addSection createInput("sk-xxx", "", true)
        }

        // API URL
        addSection(getString(R.string.model_api_url), "\u2641") {
            return@addSection createInput("https://api.openai.com/v1", "")
        }

        // Temperature
        addSection(getString(R.string.model_temp_title), "\u2668") {
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(requireContext()).apply { text = getString(R.string.model_temp_precise); setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 12f; minWidth = dp(32) })
            row.addView(SeekBar(requireContext()).apply {
                max = 100; progress = 70
                progressTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.purple, null))
                thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.purple, null))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(requireContext()).apply { text = getString(R.string.model_temp_random); setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 12f; minWidth = dp(32); gravity = Gravity.END })
            row.addView(TextView(requireContext()).apply { text = "0.70"; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 14f; setTypeface(null, Typeface.BOLD); minWidth = dp(40); gravity = Gravity.END })
            return@addSection row
        }

        // Max Token buttons
        addSection(getString(R.string.model_max_tokens), "#") {
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
            for (t in tokenOptions) {
                val isActive = t == 4096
                row.addView(TextView(requireContext()).apply {
                    text = if (t >= 1024) "${t / 1024}K" else t.toString()
                    textSize = 12f; gravity = Gravity.CENTER; setPadding(dp(8), dp(8), dp(8), dp(8))
                    setTextColor(if (isActive) resources.getColor(R.color.purple, null) else resources.getColor(R.color.gray_400, null))
                    setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
                    background = GradientDrawable().apply {
                        setColor(if (isActive) resources.getColor(R.color.purple_soft, null) else resources.getColor(R.color.bg_tertiary, null))
                        setStroke(if (isActive) 1 else 0, if (isActive) resources.getColor(R.color.purple_soft, null) else Color.TRANSPARENT)
                        cornerRadius = dp(10).toFloat()
                    }
                }, lp)
            }
            return@addSection row
        }

        // Auto understand
        addSection(getString(R.string.model_auto_title), "\u2728") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(TextView(requireContext()).apply {
                text = getString(R.string.model_auto_desc)
                setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 12f; setPadding(0, 0, 0, dp(12))
            })
            col.addView(TextView(requireContext()).apply {
                text = getString(R.string.model_auto_btn); setTextColor(Color.WHITE); textSize = 14f; setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
                background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(resources.getColor(R.color.purple, null)) }
            })
            return@addSection col
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
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_secondary, null)); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(16).toFloat() }
            elevation = 1f * resources.displayMetrics.density
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(8), dp(16), dp(0)) }
        }
        if (title.isNotEmpty()) {
            val header = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(14)) }
            if (icon.isNotEmpty()) header.addView(TextView(requireContext()).apply { text = icon; textSize = 18f; setPadding(0, 0, dp(8), 0) })
            header.addView(TextView(requireContext()).apply { text = title; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f; setTypeface(null, Typeface.BOLD) })
            card.addView(header)
        }
        card.addView(content()); root.addView(card)
    }

    private fun createInput(hint: String, value: String, isPassword: Boolean = false): EditText {
        return EditText(requireContext()).apply {
            this.hint = hint; setText(value)
            setTextColor(resources.getColor(R.color.gray_100, null)); setHintTextColor(resources.getColor(R.color.gray_500, null)); textSize = 14f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            inputType = if (isPassword) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_tertiary, null)); setStroke(1, resources.getColor(R.color.thinking_border, null)); cornerRadius = dp(10).toFloat() }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
