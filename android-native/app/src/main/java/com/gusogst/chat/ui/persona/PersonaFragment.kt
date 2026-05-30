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

    /** 从 Persona.id 获取对应描述（预设角色使用 strings.xml，自定义角色留空） */
    private fun getPersonaDescription(id: String): String = when (id) {
        "gentle" -> getString(R.string.persona_gentle_desc)
        "tsundere" -> getString(R.string.persona_tsundere_desc)
        "genki" -> getString(R.string.persona_genki_desc)
        "night" -> getString(R.string.persona_night_desc)
        "study" -> getString(R.string.persona_study_desc)
        "healing" -> getString(R.string.persona_healing_desc)
        else -> ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_persona, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        personaList = view.findViewById(R.id.personaList)
        etSearch = view.findViewById(R.id.etSearch)

        view.findViewById<ImageButton>(R.id.btnBack).apply {
            // 有返回栈时才显示返回按钮（从其他页面进来时），底部导航 Tab 时隐藏
            visibility = if (parentFragmentManager.backStackEntryCount > 0) View.VISIBLE else View.GONE
            setOnClickListener { parentFragmentManager.popBackStack() }
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

        val allPersonas = viewModel.personas.value.orEmpty()
        val filtered = allPersonas.filter { p ->
            val desc = getPersonaDescription(p.id)
            val text = "${p.name} ${p.avatar} ${p.tags.joinToString(" ")} $desc".lowercase()
            searchQuery.isEmpty() || text.contains(searchQuery)
        }

        for (p in filtered) {
            addPersonaCard(p)
        }

        // 自定义角色（从 ViewModel 加载，暂时隐藏种子）
        addCreateButton()
    }

    /** 渲染单张角色卡片（含人格特质条） */
    private fun addPersonaCard(persona: Persona) {
        val desc = getPersonaDescription(persona.id)
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
            elevation = 2f * resources.displayMetrics.density
        }

        val topRow = LinearLayout(requireContext()).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Emoji 头像
        val avatarSize = dp(48)
        val avatarBg = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resources.getColor(R.color.accent, null)) }
        topRow.addView(FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize)
            background = avatarBg
            addView(TextView(requireContext()).apply {
                text = persona.avatar; textSize = 22f; gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            })
        })

        // 名称 + 描述
        val textCol = LinearLayout(requireContext()).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(14) }
        }
        textCol.addView(TextView(requireContext()).apply {
            text = persona.name; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 16f; setTypeface(null, Typeface.BOLD)
        })
        textCol.addView(TextView(requireContext()).apply {
            text = desc; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 13f; maxLines = 1
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
        for (tag in persona.tags.take(2)) {
            tagRow.addView(TextView(requireContext()).apply {
                text = tag; setTextColor(resources.getColor(R.color.text_tertiary, null)); textSize = 11f
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = GradientDrawable().apply { cornerRadius = dp(100).toFloat(); setColor(resources.getColor(R.color.bg_tertiary, null)) }
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = dp(4) }
            })
        }
        bottomRow.addView(tagRow)

        // 右侧人格特质迷你条（取前 2 个最高 trait）
        val topTraits = getTopTraits(persona.personality, 2)
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
                setTextColor(resources.getColor(R.color.accent, null)); textSize = 10f; setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = dp(2) }
            })
            bottomRow.addView(traitChip)
        }

        card.addView(bottomRow)

        // === 点击事件：打开详情页 ===
        card.setOnClickListener {
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
            getString(R.string.trait_calm) to t.calm, getString(R.string.trait_warm) to t.warm,
            getString(R.string.trait_analytical) to t.analytical,
            getString(R.string.trait_creative) to t.creative,
            getString(R.string.trait_curious) to t.curious,
            getString(R.string.trait_precise) to t.precise,
            getString(R.string.trait_playful) to t.playful,
            getString(R.string.trait_energetic) to t.energetic
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
            text = "＋"; setTextColor(resources.getColor(R.color.accent, null)); textSize = 24f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resources.getColor(R.color.accent_soft, null)) }
        })
        val textLayout = LinearLayout(requireContext()).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(14) }
        }
        textLayout.addView(TextView(requireContext()).apply {
            text = getString(R.string.persona_create); setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 16f; setTypeface(null, Typeface.BOLD)
        })
        textLayout.addView(TextView(requireContext()).apply {
            text = getString(R.string.persona_create_desc); setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 13f
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
