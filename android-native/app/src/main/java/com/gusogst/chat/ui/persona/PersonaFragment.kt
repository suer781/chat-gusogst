package com.gusogst.chat.ui.persona

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.Persona
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var root: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sv = ScrollView(requireContext()).apply {
            setBackgroundColor(resources.getColor(R.color.bg_primary, null))
        }
        root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(100))
        }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildUI()
    }

    private fun buildUI() {
        root.removeAllViews()

        // 标题
        root.addView(TextView(requireContext()).apply {
            text = "选择角色"
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        })

        val personas = viewModel.personas.value ?: emptyList()

        for (persona in personas) {
            root.addView(createPersonaCard(persona))
        }
    }

    private fun createPersonaCard(persona: Persona): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(resources.getColor(R.color.bg_secondary, null))
            background = resources.getDrawable(R.drawable.bg_card, null)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            layoutParams = lp
            isClickable = true
            isFocusable = true
            setOnClickListener {
                viewModel.setActivePersona(persona.id)
                parentFragmentManager.popBackStack()
            }
        }

        // 头像图标
        card.addView(TextView(requireContext()).apply {
            text = persona.avatar ?: persona.emoji ?: "👤"
            textSize = 28f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                marginEnd = dp(12)
            }
        })

        // 名称和标签
        val textCol = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(requireContext()).apply {
            text = persona.name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        })
        textCol.addView(TextView(requireContext()).apply {
            text = (persona.tags ?: emptyList()).joinToString(" · ")
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 12f
            setPadding(0, dp(4), 0, 0)
        })
        card.addView(textCol)

        // 箭头
        card.addView(TextView(requireContext()).apply {
            text = "›"
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 22f
        })

        return card
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
