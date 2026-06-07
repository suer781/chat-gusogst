package com.gusogst.chat.ui.persona

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.Persona
import com.gusogst.chat.model.PersonalityTraits
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaProfileFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()

    companion object {
        private const val ARG_ID = "pid"; private const val ARG_NAME = "pname"
        private const val ARG_AVATAR = "pavatar"; private const val ARG_PROMPT = "pprompt"
        private const val ARG_TAGS = "ptags"
        private const val ARG_CALM = "pcalm"; private const val ARG_WARM = "pwarm"
        private const val ARG_ANALYTICAL = "panaly"; private const val ARG_CREATIVE = "pcreative"
        private const val ARG_CURIOUS = "pcurious"; private const val ARG_PRECISE = "pprecise"
        private const val ARG_PLAYFUL = "pplayful"; private const val ARG_ENERGETIC = "penergetic"

        fun newInstance(p: Persona) = PersonaProfileFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, p.id); putString(ARG_NAME, p.name)
                putString(ARG_AVATAR, p.avatar); putString(ARG_PROMPT, p.prompt)
                putStringArrayList(ARG_TAGS, ArrayList(p.tags))
                putFloat(ARG_CALM, p.personality.calm); putFloat(ARG_WARM, p.personality.warm)
                putFloat(ARG_ANALYTICAL, p.personality.analytical); putFloat(ARG_CREATIVE, p.personality.creative)
                putFloat(ARG_CURIOUS, p.personality.curious); putFloat(ARG_PRECISE, p.personality.precise)
                putFloat(ARG_PLAYFUL, p.personality.playful); putFloat(ARG_ENERGETIC, p.personality.energetic)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val root = (view as ScrollView).getChildAt(0) as LinearLayout
        buildUI(root)
    }

    @Suppress("DEPRECATION")
    private fun buildUI(root: LinearLayout) {
        root.removeAllViews()
        val b = arguments ?: return
        val id = b.getString("pid") ?: return
        val name = b.getString("pname") ?: ""
        val avatar = b.getString("pavatar") ?: ""
        val prompt = b.getString("pprompt") ?: ""
        val tags = b.getStringArrayList("ptags") ?: emptyList()
        val traits = PersonalityTraits(
            calm = b.getFloat("pcalm", 0.5f), warm = b.getFloat("pwarm", 0.5f),
            analytical = b.getFloat("panaly", 0.5f), creative = b.getFloat("pcreative", 0.5f),
            curious = b.getFloat("pcurious", 0.5f), precise = b.getFloat("pprecise", 0.5f),
            playful = b.getFloat("pplayful", 0.5f), energetic = b.getFloat("penergetic", 0.5f)
        )
        val persona = Persona(id = id, name = name, avatar = avatar, systemPrompt = prompt,
            tags = tags, personality = traits)

        // Header
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(16))
        }
        val avatarBg = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            colors = intArrayOf(resources.getColor(R.color.accent, null), resources.getColor(R.color.accent_hover, null))
            orientation = GradientDrawable.Orientation.TL_BR
        }
        header.addView(FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply { marginEnd = dp(12) }
            background = avatarBg
            addView(TextView(requireContext()).apply {
                text = avatar.ifEmpty { name.first().toString() }; textSize = 28f
                gravity = Gravity.CENTER; setTextColor(resources.getColor(R.color.white, null))
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            })
        })
        val textCol = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(requireContext()).apply {
            text = name; setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 20f; setTypeface(null, Typeface.BOLD)
        })
        textCol.addView(TextView(requireContext()).apply {
            text = tags.joinToString(" · "); setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 12f; setPadding(0, dp(4), 0, 0)
        })
        header.addView(textCol)
        header.addView(TextView(requireContext()).apply {
            text = "\u2699"; textSize = 18f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginStart = dp(8) }
            background = GradientDrawable().apply {
                setColor(resources.getColor(R.color.bg_secondary, null))
                setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(10).toFloat()
            }
            setOnClickListener { showSettingsDialog(persona) }
        })
        root.addView(header)

        // Personality traits
        root.addView(TextView(requireContext()).apply {
            text = "人格特质"; setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f; setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
        })
        val traitDefs = listOf(
            "冷静" to traits.calm, "温暖" to traits.warm, "分析" to traits.analytical,
            "创造" to traits.creative, "好奇" to traits.curious, "精准" to traits.precise,
            "风趣" to traits.playful, "活力" to traits.energetic
        )
        for ((tname, value) in traitDefs) {
            val trow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) }
            }
            trow.addView(TextView(requireContext()).apply {
                text = tname; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 13f
                layoutParams = LinearLayout.LayoutParams(dp(50), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            val barH = dp(10)
            val barContainer = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, barH, 1f).apply { marginStart = dp(4); marginEnd = dp(4) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE; cornerRadius = barH / 2f
                    setColor(resources.getColor(R.color.bg_tertiary, null))
                }
            }
            val fill = View(requireContext())
            fill.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = barH / 2f
                setColor(resources.getColor(R.color.accent, null))
            }
            barContainer.addView(fill)
            fill.post {
                val w = (barContainer.width * value).toInt().coerceAtLeast(0)
                fill.layoutParams = FrameLayout.LayoutParams(w, barH)
                fill.requestLayout()
            }
            trow.addView(barContainer)
            trow.addView(TextView(requireContext()).apply {
                text = "${(value * 100).toInt()}%"
                setTextColor(resources.getColor(R.color.accent, null)); textSize = 12f; setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            root.addView(trow)
        }

        // System prompt
        root.addView(TextView(requireContext()).apply {
            text = "系统提示词"; setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f; setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16); bottomMargin = dp(8) }
        })
        root.addView(TextView(requireContext()).apply {
            text = prompt.ifEmpty { "No system prompt set" }
            setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f; setLineSpacing(0f, 1.6f)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_secondary, null)); cornerRadius = dp(10).toFloat() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150))
        })

        // Buttons
        val btnRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(20), 0, 0) }
        btnRow.addView(TextView(requireContext()).apply {
            text = "返回"; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 14f
            gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.transparent, null)); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(10).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(10) }
            setOnClickListener { parentFragmentManager.popBackStack() }
        })
        btnRow.addView(TextView(requireContext()).apply {
            text = "开始对话"; setTextColor(resources.getColor(R.color.white, null)); textSize = 14f; setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.accent, null)); cornerRadius = dp(10).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                viewModel.setActivePersona(id)
                parentFragmentManager.popBackStack()
                parentFragmentManager.popBackStack()
            }
        })
        root.addView(btnRow)
    }

    private fun showSettingsDialog(p: Persona) {
        PersonaSettingsDialog.newInstance(p).show(parentFragmentManager, "settings")
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
