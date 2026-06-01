package com.gusogst.chat.ui.persona

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
import com.gusogst.chat.model.PersonalityTraits
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaSettingsDialog : DialogFragment() {
    private val viewModel: ChatViewModel by activityViewModels()

    private var prompt = ""
    private var temperature = 0.7f
    private var topP = 0.9f
    private var maxTokens = 2048
    private var autoMode = "off" // off / rule / llm
    private var personaId = ""
    private var personaName = ""
    private var personaAvatar = ""
    private var overrideGlobal = false
    private var traits = PersonalityTraits()

    private fun createEmptyDialog(): Dialog {
        return Dialog(requireContext()).apply {
            setContentView(TextView(requireContext()).apply {
                text = "Error loading persona"; setPadding(40, 40, 40, 40)
            })
        }
    }

    companion object {
        private const val ARG_ID = "pid"; private const val ARG_NAME = "pname"
        private const val ARG_AVATAR = "pavatar"; private const val ARG_PROMPT = "pprompt"
        private const val ARG_CALM = "pcalm"; private const val ARG_WARM = "pwarm"
        private const val ARG_ANALYTICAL = "panaly"; private const val ARG_CREATIVE = "pcreative"
        private const val ARG_CURIOUS = "pcurious"; private const val ARG_PRECISE = "pprecise"
        private const val ARG_PLAYFUL = "pplayful"; private const val ARG_ENERGETIC = "penergetic"

        fun newInstance(p: Persona) = PersonaSettingsDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, p.id); putString(ARG_NAME, p.name)
                putString(ARG_AVATAR, p.avatar); putString(ARG_PROMPT, p.prompt)
                putFloat(ARG_CALM, p.personality.calm); putFloat(ARG_WARM, p.personality.warm)
                putFloat(ARG_ANALYTICAL, p.personality.analytical); putFloat(ARG_CREATIVE, p.personality.creative)
                putFloat(ARG_CURIOUS, p.personality.curious); putFloat(ARG_PRECISE, p.personality.precise)
                putFloat(ARG_PLAYFUL, p.personality.playful); putFloat(ARG_ENERGETIC, p.personality.energetic)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = arguments ?: return createEmptyDialog()
        personaId = b.getString("pid") ?: ""
        personaName = b.getString("pname") ?: ""
        personaAvatar = b.getString("pavatar") ?: ""
        prompt = b.getString("pprompt") ?: ""
        traits = PersonalityTraits(
            calm = b.getFloat("pcalm", 0.5f), warm = b.getFloat("pwarm", 0.5f),
            analytical = b.getFloat("panaly", 0.5f), creative = b.getFloat("pcreative", 0.5f),
            curious = b.getFloat("pcurious", 0.5f), precise = b.getFloat("pprecise", 0.5f),
            playful = b.getFloat("pplayful", 0.5f), energetic = b.getFloat("penergetic", 0.5f)
        )
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_bg))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        // Header
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))
        }
        header.addView(TextView(requireContext()).apply {
            text = "角色设置"; setTextColor(resources.getColor(R.color.white, null)); textSize = 18f; setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(requireContext()).apply {
            text = "✕"; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 18f
            setPadding(dp(8), dp(8), dp(8), dp(8)); setOnClickListener { dismiss() }
        })
        root.addView(header)

        val sv = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val content = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(8)) }

        // Prompt editor
        content.addView(sectionLabel("📝 系统提示词"))
        val promptInput = EditText(requireContext()).apply {
            setText(prompt); setTextColor(resources.getColor(R.color.gray_100, null))
            setHintTextColor(resources.getColor(R.color.gray_500, null)); hint = "描述角色的性格、风格、行为准则..."
            textSize = 14f; setPadding(dp(14), dp(12), dp(14), dp(12)); minLines = 4; maxLines = 8
            background = GradientDrawable().apply {
                setColor(resources.getColor(R.color.bg_tertiary, null)); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(8).toFloat()
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { prompt = s?.toString() ?: "" }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        content.addView(promptInput)

        // Override global toggle
        content.addView(sectionLabel("⚙ 独立参数"))
        val overrideRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8))
            setOnClickListener { overrideGlobal = !overrideGlobal; dismiss(); showSettingsAgain() }
        }
        overrideRow.addView(TextView(requireContext()).apply {
            text = if (overrideGlobal) "使用本角色独立参数" else "跟随全局模型设置"
            setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val toggle = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(26))
            background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(if (overrideGlobal) resources.getColor(R.color.accent, null) else ContextCompat.getColor(requireContext(), R.color.overlay_light_1A)) }
        }
        toggle.addView(View(requireContext()).apply {
            val lp = FrameLayout.LayoutParams(dp(22), dp(22)); lp.setMargins(dp(if (overrideGlobal) 22 else 2), dp(2), 0, 0); layoutParams = lp
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resources.getColor(R.color.white, null)) }
        })
        overrideRow.addView(toggle)
        content.addView(overrideRow)

        // Quick presets
        content.addView(sectionLabel("⚡ 快捷预设"))
        val presetRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)) }
        presetRow.addView(createPresetBtn("🔧 规则引擎") {
            val adj = analyzeWithRules(prompt)
            temperature = (temperature + adj[0]).coerceIn(0f, 2f)
            topP = (topP + adj[1]).coerceIn(0f, 1f)
            maxTokens = (maxTokens + adj[2].toInt()).coerceIn(100, 8000)
        })
        presetRow.addView(createPresetBtn("🧠 自主理解") {
            val fallback = analyzeWithLLM(prompt)
            temperature = fallback[0]; topP = fallback[1]; maxTokens = fallback[2].toInt()
            autoMode = "llm"
        })
        content.addView(presetRow)

        // Sliders
        content.addView(createSlider("创造力", temperature, 0f, 2f, 0.1f) { temperature = it })
        content.addView(createSlider("多样性", topP, 0f, 1f, 0.05f) { topP = it })
        content.addView(createSlider("回复长度", maxTokens.toFloat(), 100f, 8000f, 100f) { maxTokens = it.toInt() })

        // Auto mode
        content.addView(sectionLabel("聊天时自动调节"))
        val modeRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)) }
        for ((key, label) in listOf("off" to "关闭", "rule" to "规则", "llm" to "LLM")) {
            modeRow.addView(createModeBtn(label, key == autoMode) { autoMode = key })
        }
        content.addView(modeRow)

        // Personality traits
        content.addView(sectionLabel("🎭 人格特质"))
        val traitDefs = listOf(
            "冷静" to traits.calm, "温暖" to traits.warm, "分析" to traits.analytical,
            "创造" to traits.creative, "好奇" to traits.curious, "精准" to traits.precise,
            "风趣" to traits.playful, "活力" to traits.energetic
        )
        val traitVals = traitDefs.map { it.second }.toMutableList()
        for ((i, pair) in traitDefs.withIndex()) {
            val idx = i
            content.addView(createTraitSlider(pair.first, pair.second) { traitVals[idx] = it })
        }

        sv.addView(content)
        root.addView(sv)

        // Buttons
        val btnRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(16), 0, 0) }
        btnRow.addView(TextView(requireContext()).apply {
            text = "取消"; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 14f; gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.transparent, null)); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(10).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(10) }
            setOnClickListener { dismiss() }
        })
        btnRow.addView(TextView(requireContext()).apply {
            text = "保存设置"; setTextColor(resources.getColor(R.color.white, null)); textSize = 14f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(resources.getColor(R.color.accent, null)) }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                val newTraits = PersonalityTraits(
                    calm = traitVals[0], warm = traitVals[1], analytical = traitVals[2],
                    creative = traitVals[3], curious = traitVals[4], precise = traitVals[5],
                    playful = traitVals[6], energetic = traitVals[7]
                )
                viewModel.updatePersona(personaId) { p ->
                    p.copy(prompt = prompt, personality = newTraits,
                        modelParamsConfig = ModelParamsConfig(
                            autoMode = autoMode, overrideGlobal = overrideGlobal,
                            temperature = temperature, topP = topP, maxTokens = maxTokens
                        ))
                }
                dismiss()
            }
        })
        root.addView(btnRow)

        val dlg = Dialog(requireContext())
        dlg.setContentView(root)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.transparent, null)))
            setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), (resources.displayMetrics.heightPixels * 0.85).toInt())
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.5f)
        }
        return dlg
    }

    private fun showSettingsAgain() {
        PersonaSettingsDialog().apply {
            arguments = Bundle().apply {
                putString("pid", personaId); putString("pname", personaName)
                putString("pavatar", personaAvatar); putString("pprompt", prompt)
                putFloat("pcalm", traits.calm); putFloat("pwarm", traits.warm)
                putFloat("panaly", traits.analytical); putFloat("pcreative", traits.creative)
                putFloat("pcurious", traits.curious); putFloat("pprecise", traits.precise)
                putFloat("pplayful", traits.playful); putFloat("penergetic", traits.energetic)
            }
        }.show(parentFragmentManager, "settings2")
    }

    private fun sectionLabel(text: String): TextView {
        val d = resources.displayMetrics.density
        return TextView(requireContext()).apply {
            this.text = text; setTextColor(resources.getColor(R.color.gray_100, null)); textSize = 14f
            setTypeface(null, Typeface.BOLD); setPadding(0, (16 * d).toInt(), 0, (8 * d).toInt())
        }
    }

    private fun createPresetBtn(label: String, onClick: () -> Unit): TextView {
        val d = resources.displayMetrics.density
        return TextView(requireContext()).apply {
            text = label; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 13f
            gravity = Gravity.CENTER; setPadding((8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt())
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_tertiary, null)); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = (8 * d).toFloat() }
            setOnClickListener { onClick() }
        }
    }

    private fun createSlider(label: String, value: Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit): LinearLayout {
        val d = resources.displayMetrics.density
        val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, (8 * d).toInt(), 0, (8 * d).toInt()) }
        col.addView(TextView(requireContext()).apply {
            this.text = label; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 14f
            setPadding(0, 0, 0, (6 * d).toInt())
        })
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(requireContext()).apply { text = String.format("%.2f", min); setTextColor(resources.getColor(R.color.gray_500, null)); textSize = 11f; minWidth = (32 * d).toInt() })
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
        row.addView(TextView(requireContext()).apply { text = String.format("%.2f", max); setTextColor(resources.getColor(R.color.gray_500, null)); textSize = 11f; minWidth = (32 * d).toInt(); gravity = Gravity.END })
        col.addView(row)
        col.addView(TextView(requireContext()).apply {
            text = String.format("%.2f", value); setTextColor(resources.getColor(R.color.gray_200, null)); textSize = 13f; gravity = Gravity.CENTER; setPadding(0, (4 * d).toInt(), 0, 0)
        })
        return col
    }

    private fun createModeBtn(label: String, active: Boolean, onClick: () -> Unit): TextView {
        val d = resources.displayMetrics.density
        return TextView(requireContext()).apply {
            text = label; textSize = 13f; gravity = Gravity.CENTER
            setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
            setTextColor(if (active) resources.getColor(R.color.accent, null) else resources.getColor(R.color.gray_400, null))
            background = GradientDrawable().apply {
                setColor(if (active) ContextCompat.getColor(requireContext(), R.color.accent_soft) else resources.getColor(R.color.bg_tertiary, null))
                setStroke(if (active) 1 else 0, if (active) resources.getColor(R.color.accent, null) else resources.getColor(R.color.transparent, null))
                cornerRadius = (8 * d).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = (6 * d).toInt() }
            setOnClickListener { onClick() }
        }
    }

    private fun createTraitSlider(label: String, value: Float, onChange: (Float) -> Unit): LinearLayout {
        val d = resources.displayMetrics.density
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (6 * d).toInt(), 0, (6 * d).toInt())
        }
        row.addView(TextView(requireContext()).apply {
            text = label; setTextColor(resources.getColor(R.color.gray_300, null)); textSize = 13f
            layoutParams = LinearLayout.LayoutParams((50 * d).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        val pctTv = TextView(requireContext()).apply {
            text = "${(value * 100).toInt()}%"; setTextColor(resources.getColor(R.color.accent, null)); textSize = 12f
            layoutParams = LinearLayout.LayoutParams((36 * d).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            setTypeface(null, Typeface.BOLD)
        }
        val seek = SeekBar(requireContext()).apply {
            max = 100; progress = (value * 100).toInt()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = (4 * d).toInt(); marginEnd = (4 * d).toInt() }
            progressTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
            thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) { onChange(progress / 100f); pctTv.text = "${progress}%" } }
                override fun onStartTrackingTouch(sb: SeekBar?) {}; override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        row.addView(seek)
        row.addView(pctTv)
        return row
    }

    private fun analyzeWithRules(prompt: String): FloatArray {
        val lower = prompt.lowercase()
        var tAdj = 0f; var pAdj = 0f; var tokAdj = 0f
        if (lower.containsAny(listOf("幽默", "开玩笑", "活泼", "跳跃"))) { tAdj += 0.3f; pAdj += 0.1f }
        if (lower.containsAny(listOf("严谨", "专业", "正式", "学术"))) { tAdj -= 0.2f; pAdj -= 0.1f }
        if (lower.containsAny(listOf("话多", "详细", "多说", "展开"))) { tokAdj += 1000f }
        if (lower.containsAny(listOf("简洁", "答案", "精炼"))) { tokAdj -= 500f }
        return floatArrayOf(tAdj, pAdj, tokAdj)
    }

    private fun analyzeWithLLM(prompt: String): FloatArray {
        val lower = prompt.lowercase()
        var t = 0.7f; var p = 0.9f; var tok = 2048f
        if (lower.containsAny(listOf("创意", "写作", "故事", "诗歌"))) { t = 1.2f; p = 0.95f; tok = 4096f }
        if (lower.containsAny(listOf("代码", "编程", "技术"))) { t = 0.3f; p = 0.8f; tok = 4096f }
        if (lower.containsAny(listOf("聊天", "陪伴", "清暖"))) { t = 0.9f; p = 0.95f; tok = 2048f }
        return floatArrayOf(t, p, tok)
    }

    private fun String.containsAny(keywords: List<String>): Boolean = keywords.any { this.contains(it) }
}
