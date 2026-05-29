package com.gusogst.chat.ui.persona

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.Persona
import com.gusogst.chat.model.PersonalityTraits
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaFragment : Fragment() {

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var personaList: LinearLayout
    private lateinit var etSearch: EditText

    private var searchQuery: String = ""

    // 预设角色（同步 Web 主分支 persona.ts）
    data class PresetPersona(
        val id: String, val name: String, val emoji: String,
        val description: String, val tags: List<String>,
        val personality: PersonalityTraits
    )

    private val presets = listOf(
        PresetPersona("gentle", "温柔型", "💕",
            "性格温柔体贴，说话轻声细语",
            listOf("日常", "温柔"),
            PersonalityTraits(warm = 0.9f, calm = 0.6f)),
        PresetPersona("tsundere", "傲娇型", "💢",
            "嘴上说不关心其实很在乎",
            listOf("日常", "傲娇"),
            PersonalityTraits(playful = 0.8f, energetic = 0.6f)),
        PresetPersona("genki", "元气型", "☀️",
            "活泼开朗，充满正能量",
            listOf("日常", "元气"),
            PersonalityTraits(energetic = 0.9f, playful = 0.7f)),
        PresetPersona("night", "深夜谈心", "🌙",
            "安静有深度的深夜聊天",
            listOf("深夜", "谈心"),
            PersonalityTraits(calm = 0.8f, warm = 0.7f)),
        PresetPersona("study", "陪伴学习", "📚",
            "学习工作时的温柔陪伴",
            listOf("学习", "陪伴"),
            PersonalityTraits(calm = 0.7f, warm = 0.6f)),
        PresetPersona("healing", "治愈安慰", "🫂",
            "难过时的温暖安慰",
            listOf("安慰", "治愈"),
            PersonalityTraits(warm = 0.9f, calm = 0.8f))
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_persona, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        personaList = view.findViewById(R.id.personaList)
        etSearch = view.findViewById(R.id.etSearch)

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                buildCards()
            }
        })

        viewModel.personas.observe(viewLifecycleOwner) { buildCards() }
    }

    private fun buildCards() {
        personaList.removeAllViews()

        val filtered = presets.filter { p ->
            val text = "${p.name} ${p.emoji} ${p.tags.joinToString(" ")} ${p.description}".lowercase()
            searchQuery.isEmpty() || text.contains(searchQuery)
        }

        for (p in filtered) {
            addPersonaCard(p)
        }

        // 自定义角色（从 ViewModel 加载，暂时隐藏种子）
        addCreateButton()
    }

    /** 渲染单张角色卡片（含人格特质条） */
    private fun addPersonaCard(preset: PresetPersona) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val bg = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(resources.getColor(R.color.bg_secondary, null))
                setStroke(1, resources.getColor(R.color.bg_tertiary, null))
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(8) }
            isClickable = true
            isFocusable = true
        }

        // === 上部分：头像 + 名称 + 描述 ===
        val topRow = LinearLayout(requireContext()).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Emoji 头像
        val avatarSize = dp(48)
        val avatarBg = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#E94560")) }
        topRow.addView(FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize)
            background = avatarBg
            addView(TextView(requireContext()).apply {
                text = preset.emoji; textSize = 22f; gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            })
        })

        // 名称 + 描述
        val textCol = LinearLayout(requireContext()).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(14) }
        }
        textCol.addView(TextView(requireContext()).apply {
            text = preset.name; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 16f; setTypeface(null, Typeface.BOLD)
        })
        textCol.addView(TextView(requireContext()).apply {
            text = preset.description; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 13f; maxLines = 1
        })
        topRow.addView(textCol)
        // 箭头 >
        topRow.addView(TextView(requireContext()).apply {
            text = ">"; setTextColor(resources.getColor(R.color.text_tertiary, null)); textSize = 18f
        })
        card.addView(topRow)

        // === 下部分：标签 + 人格特质条 ===
        val bottomRow = LinearLayout(requireContext()).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) }
        }

        // 左侧标签
        val tagRow = LinearLayout(requireContext()).apply {
            orientation = HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        for (tag in preset.tags.take(2)) {
            tagRow.addView(TextView(requireContext()).apply {
                text = tag; setTextColor(resources.getColor(R.color.text_tertiary, null)); textSize = 11f
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = GradientDrawable().apply { cornerRadius = dp(100).toFloat(); setColor(resources.getColor(R.color.bg_tertiary, null)) }
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = dp(4) }
            })
        }
        bottomRow.addView(tagRow)

        // 右侧人格特质迷你条（取前 2 个最高 trait）
        val topTraits = getTopTraits(preset.personality, 2)
        for ((traitName, value) in topTraits) {
            val traitChip = LinearLayout(requireContext()).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = dp(6) }
            }
            traitChip.addView(TextView(requireContext()).apply {
                text = traitName; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 10f
            })
            // 迷你进度条
            val barW = dp(40); val barH = dp(4)
            traitChip.addView(ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; progress = (value * 100).toInt()
                layoutParams = LinearLayout.LayoutParams(barW, barH).apply { marginStart = dp(3) }
                progressDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE; cornerRadius = barH / 2f
                    setColor(resources.getColor(R.color.bg_tertiary, null))
                }
                // 使用层叠进度条
                isIndeterminate = false
            })
            traitChip.addView(TextView(requireContext()).apply {
                text = "${(value * 100).toInt()}%"
                setTextColor(Color.parseColor("#E94560")); textSize = 10f; setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = dp(2) }
            })
            bottomRow.addView(traitChip)
        }

        card.addView(bottomRow)

        // === 点击事件：打开详情页 ===
        card.setOnClickListener {
            val persona = Persona(
                id = preset.id, name = preset.name, avatar = preset.emoji,
                prompt = "You are ${preset.name}. ${preset.description}",
                tags = preset.tags, personality = preset.personality
            )
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, PersonaProfileFragment.newInstance(persona))
                .addToBackStack(null)
                .commit()
        }

        personaList.addView(card)
    }

    /** 取人格特质中 Top N（按值排序） */
    private fun getTopTraits(t: PersonalityTraits, n: Int): List<Pair<String, Float>> {
        return listOf(
            "冷静" to t.calm, "温暖" to t.warm, "分析" to t.analytical,
            "创造" to t.creative, "好奇" to t.curious, "精准" to t.precise,
            "风趣" to t.playful, "活力" to t.energetic
        ).sortedByDescending { it.second }.take(n)
    }

    private fun addCreateButton() {
        val card = LinearLayout(requireContext()).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat(); setColor(Color.TRANSPARENT)
                setStroke(1, resources.getColor(R.color.text_tertiary, null))
            }
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(4) }
        }
        card.addView(TextView(requireContext()).apply {
            text = "＋"; setTextColor(Color.parseColor("#E94560")); textSize = 24f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#1AE94560")) }
        })
        val textLayout = LinearLayout(requireContext()).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(14) }
        }
        textLayout.addView(TextView(requireContext()).apply {
            text = "创建角色"; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 16f; setTypeface(null, Typeface.BOLD)
        })
        textLayout.addView(TextView(requireContext()).apply {
            text = "自定义你的AI伙伴"; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 13f
        })
        card.addView(textLayout)
        card.addView(TextView(requireContext()).apply {
            text = ">"; setTextColor(resources.getColor(R.color.text_tertiary, null)); textSize = 18f
        })
        card.setOnClickListener {
            // 打开创建页面
            val defaultPersona = Persona(
                name = "Custom", avatar = "🎭", prompt = "You are a helpful assistant.",
                tags = listOf("custom"), personality = PersonalityTraits()
            )
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, PersonaProfileFragment.newInstance(defaultPersona))
                .addToBackStack(null)
                .commit()
        }
        personaList.addView(card)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    companion object { const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT; const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT; const val HORIZONTAL = LinearLayout.HORIZONTAL; const val VERTICAL = LinearLayout.VERTICAL }
}
