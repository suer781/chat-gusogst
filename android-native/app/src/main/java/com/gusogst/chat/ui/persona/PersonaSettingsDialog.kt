package com.gusogst.chat.ui.persona

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.ModelParamsConfig
import com.gusogst.chat.model.Persona
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaSettingsDialog : DialogFragment() {
    private val viewModel: ChatViewModel by activityViewModels()

    private var prompt = ""
    private var temperature = 0.7f
    private var topP = 0.9f
    private var maxTokens = 2048
    private var overrideGlobal = false
    private var autoMode = "off" // off / rule / llm
    private var personaId = ""

    companion object {
        private const val ARG_PERSONA_ID = "persona_id"
        fun newInstance(personaId: String) = PersonaSettingsDialog().apply {
            arguments = Bundle().apply { putString(ARG_PERSONA_ID, personaId) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        personaId = arguments?.getString(ARG_PERSONA_ID) ?: ""
        val persona = viewModel.personas.value.orEmpty().find { it.id == personaId }
        persona?.let {
            prompt = it.prompt
            it.modelParamsConfig?.let { cfg ->
                temperature = cfg.temperature
                topP = cfg.topP
                maxTokens = cfg.maxTokens
                overrideGlobal = cfg.overrideGlobal
                autoMode = cfg.autoMode
            }
        }

        val dp = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * dp).toInt()

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#151525"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        // Header
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))
        }
        header.addView(TextView(requireContext()).apply {
            text = "\u89D2\u8272\u8BBE\u7F6E"; setTextColor(Color.WHITE); textSize = 18f; setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(requireContext()).apply {
            text = "\u2715"; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 18f
            setPadding(dp(8), dp(8), dp(8), dp(8)); setOnClickListener { dismiss() }
        })
        root.addView(header)

        val sv = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val content = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(8)) }

        // Prompt editor
        content.addView(sectionLabel("\uD83D\uDCDD \u7CFB\u7EDF\u63D0\u793A\u8BCD"))
        val promptInput = EditText(requireContext()).apply {
            setText(prompt); setTextColor(resources.getColor(R.color.gray_100, null))
            setHintTextColor(resources.getColor(R.color.gray_500, null)); hint = "\u63CF\u8FF0\u89D2\u8272\u7684\u6027\u683C\u3001\u98CE\u683C\u3001\u884C\u4E3A\u51C6\u5219..."
            textSize = 14f; setPadding(dp(14), dp(12), dp(14), dp(12)); minLines = 4; maxLines = 8
            background = GradientDrawable().apply {
                setColor(resources.getColor(R.color.bg_tertiary, null)); setStroke(1, Color.parseColor("#2A2A5A")); cornerRadius = dp(8).toFloat()
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { prompt = s?.toString() ?: "" }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        content.addView(promptInput)

        // Override global toggle
        content.addView(sectionLabel("\u2699 \u72EC\u7ACB\u53C2\u6570"))
        val overrideRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8))
            setOnClickListener { overrideGlobal = !overrideGlobal; buildOverrideUI(content) }
        }
        overrideRow.addView(TextView(requireContext()).apply {
            text = if (overrideGlobal) "\u4F7F\u7528\u672C\u89D2\u8272\u72EC\u7ACB\u53C2\u6570" else "\u8DDF\u968F\u5168\u5C40\u6A21\u578B\u8BBE\u7F6E"
            setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val toggle = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(26))
            background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(if (overrideGlobal) resources.getColor(R.color.accent, null) else Color.parseColor("#1AFFFFFF")) }
        }
        toggle.addView(View(requireContext()).apply {
            val lp = FrameLayout.LayoutParams(dp(22), dp(22)); lp.setMargins(dp(if (overrideGlobal) 22 else 2), dp(2), 0, 0); layoutParams = lp
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) }
        })
        overrideRow.addView(toggle)
        content.addView(overrideRow)

        // Quick presets
        content.addView(sectionLabel("\u26A1 \u5FEB\u6377\u9884\u8BBE"))
        val presetRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)) }
        presetRow.addView(createPresetBtn("\uD83D\uDD27 \u89C4\u5219\u5F15\u64CE") {
            // Rule engine: adjust sliders based on prompt keywords
            val adj = analyzeWithRules(prompt)
            temperature = (temperature + adj[0]).coerceIn(0f, 2f)
            topP = (topP + adj[1]).coerceIn(0f, 1f)
            maxTokens = (maxTokens + adj[2].toInt()).coerceIn(100, 8000)
        })
        presetRow.addView(createPresetBtn("\uD83E\uDDE0 \u81EA\u4E3B\u7406\u89E3") {
            // LLM analysis: use local fallback
            val fallback = analyzeWithLLM(prompt)
            temperature = fallback[0]
            topP = fallback[1]
            maxTokens = fallback[2].toInt()
            autoMode = "llm"
        })
        content.addView(presetRow)

        // Sliders
        content.addView(createSlider("\u521B\u9020\u529B", temperature, 0f, 2f, 0.1f) { temperature = it })
        content.addView(createSlider("\u591A\u6837\u6027", topP, 0f, 1f, 0.05f) { topP = it })
        content.addView(createSlider("\u56DE\u590D\u957F\u5EA6", maxTokens.toFloat(), 100f, 8000f, 100f) { maxTokens = it.toInt() })

        // Auto mode
        content.addView(sectionLabel("\u804A\u5929\u65F6\u81EA\u52A8\u8C03\u8282"))
        val modeRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)) }
        for ((key, label) in listOf("off" to "\u5173\u95ED", "rule" to "\u89C4\u5219", "llm" to "LLM")) {
            modeRow.addView(createModeBtn(label, key == autoMode) { autoMode = key })
        }
        content.addView(modeRow)

        sv.addView(content)
        root.addView(sv)

        // Buttons
        val btnRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(16), 0, 0) }
        btnRow.addView(TextView(requireContext()).apply {
            text = "\u53D6\u6D88"; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 14f; gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT); setStroke(1, Color.parseColor("#2A2A5A")); cornerRadius = dp(10).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(10) }
            setOnClickListener { dismiss() }
        })
        btnRow.addView(TextView(requireContext()).apply {
            text = "\u4FDD\u5B58\u8BBE\u7F6E"; setTextColor(Color.WHITE); textSize = 14f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(resources.getColor(R.color.accent, null)) }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                viewModel.updatePersona(personaId) { p ->
                    p.copy(prompt = prompt, modelParamsConfig = ModelParamsConfig(
                        autoMode = autoMode, overrideGlobal = overrideGlobal,
                        temperature = temperature, topP = topP, maxTokens = maxTokens
                    ))
                }
                dismiss()
            }
        })
        root.addView(btnRow)

        return Dialog(requireContext()).apply {
            setContentView(root)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), (resources.displayMetrics.heightPixels * 0.85).toInt())
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0.5f)
            }
        }
    }

    private fun sectionLabel(text: String): TextView {
        val dp = resources.displayMetrics.density
        return TextView(requireContext()).apply {
            this.text = text; setTextColor(resources.getColor(R.color.gray_100, null)); textSize = 14f
            setTypeface(null, Typeface.BOLD); setPadding(0, (16 * dp).toInt(), 0, (8 * dp).toInt())
        }
    }

    private fun createPresetBtn(label: String, onClick: () -> Unit): TextView {
        val dp = resources.displayMetrics.density
        return TextView(requireContext()).apply {
            text = label; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 13f
            gravity = Gravity.CENTER; setPadding((8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt())
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_tertiary, null)); setStroke(1, Color.parseColor("#2A2A5A")); cornerRadius = (8 * dp).toFloat() }
            setOnClickListener { onClick() }
        }
    }

    private fun createSlider(label: String, value: Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit): LinearLayout {
        val dp = resources.displayMetrics.density
        val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt()) }
        col.addView(TextView(requireContext()).apply {
            this.text = label; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 14f
            setPadding(0, 0, 0, (6 * dp).toInt())
        })
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(requireContext()).apply { text = String.format("%.2f", min); setTextColor(resources.getColor(R.color.gray_500, null)); textSize = 11f; minWidth = (32 * dp).toInt() })
        row.addView(SeekBar(requireContext()).apply {
            setMax(((max - min) / step).toInt()); progress = ((value - min) / step).toInt()
            progressTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
            thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) onChange(min + progress * step) }
                override fun onStartTrackingTouch(sb: SeekBar?) {}; override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })
        row.addView(TextView(requireContext()).apply { text = String.format("%.2f", max); setTextColor(resources.getColor(R.color.gray_500, null)); textSize = 11f; minWidth = (32 * dp).toInt(); gravity = Gravity.END })
        col.addView(row)
        col.addView(TextView(requireContext()).apply {
            text = String.format("%.2f", value); setTextColor(resources.getColor(R.color.gray_200, null)); textSize = 13f; gravity = Gravity.CENTER; setPadding(0, (4 * dp).toInt(), 0, 0)
        })
        return col
    }

    private fun createModeBtn(label: String, active: Boolean, onClick: () -> Unit): TextView {
        val dp = resources.displayMetrics.density
        return TextView(requireContext()).apply {
            text = label; textSize = 13f; gravity = Gravity.CENTER
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            setTextColor(if (active) resources.getColor(R.color.accent, null) else resources.getColor(R.color.gray_400, null))
            background = GradientDrawable().apply {
                setColor(if (active) Color.parseColor("#1AE94560") else resources.getColor(R.color.bg_tertiary, null))
                setStroke(if (active) 1 else 0, if (active) resources.getColor(R.color.accent, null) else Color.TRANSPARENT)
                cornerRadius = (8 * dp).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = (6 * dp).toInt() }
            setOnClickListener { onClick() }
        }
    }

    private fun buildOverrideUI(content: LinearLayout) { /* Rebuild toggle state on change */ }

    // Rule engine: keyword -> slider adjustments
    private fun analyzeWithRules(prompt: String): FloatArray {
        val lower = prompt.lowercase()
        var tAdj = 0f; var pAdj = 0f; var tokAdj = 0f
        if (lower.containsAny(listOf("\u5E7D\u9ED8", "\u5F00\u73A9\u7B11", "\u6D3B\u6CFC", "\u8DF3\u8DC3"))) { tAdj += 0.3f; pAdj += 0.1f }
        if (lower.containsAny(listOf("\u4E25\u8C28", "\u4E13\u4E1A", "\u6B63\u5F0F", "\u5B66\u672F"))) { tAdj -= 0.2f; pAdj -= 0.1f }
        if (lower.containsAny(listOf("\u8BDD\u591A", "\u8BE6\u7EC6", "\u591A\u8BF4", "\u5C55\u5F00"))) { tokAdj += 1000f }
        if (lower.containsAny(listOf("\u7B80\u6D01", "\u7B54\u6848", "\u7CBE\u70BC"))) { tokAdj -= 500f }
        return floatArrayOf(tAdj, pAdj, tokAdj)
    }

    // LLM fallback: local analysis
    private fun analyzeWithLLM(prompt: String): FloatArray {
        val lower = prompt.lowercase()
        var t = 0.7f; var p = 0.9f; var tok = 2048f
        if (lower.containsAny(listOf("\u521B\u610F", "\u5199\u4F5C", "\u6545\u4E8B", "\u8BD7\u6B4C"))) { t = 1.2f; p = 0.95f; tok = 4096f }
        if (lower.containsAny(listOf("\u4EE3\u7801", "\u7F16\u7A0B", "\u6280\u672F"))) { t = 0.3f; p = 0.8f; tok = 4096f }
        if (lower.containsAny(listOf("\u804A\u5929", "\u966B\u4F34", "\u6E05\u6696"))) { t = 0.9f; p = 0.95f; tok = 2048f }
        return floatArrayOf(t, p, tok)
    }

    private fun String.containsAny(keywords: List<String>): Boolean = keywords.any { this.contains(it) }
}
