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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.Persona
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaProfileFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()

    companion object {
        private const val ARG_PERSONA_ID = "persona_id"
        fun newInstance(personaId: String) = PersonaProfileFragment().apply {
            arguments = Bundle().apply { putString(ARG_PERSONA_ID, personaId) }
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

    private fun buildUI(root: LinearLayout) {
        root.removeAllViews()
        val personaId = arguments?.getString(ARG_PERSONA_ID) ?: return
        val personas = viewModel.personas.value.orEmpty()
        val persona = personas.find { it.id == personaId } ?: return

        // Header: 64x64 avatar + name + traits
        val header = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(16)) }
        val avatarBg = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            colors = intArrayOf(resources.getColor(R.color.accent, null), resources.getColor(R.color.accent_hover, null))
            orientation = GradientDrawable.Orientation.TL_BR
        }
        header.addView(FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply { marginEnd = dp(12) }
            background = avatarBg
            addView(TextView(requireContext()).apply {
                text = persona.avatar.ifEmpty { persona.name.first().toString() }; textSize = 28f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            })
        })
        val textCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        textCol.addView(TextView(requireContext()).apply {
            text = persona.name; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 20f; setTypeface(null, Typeface.BOLD)
        })
        textCol.addView(TextView(requireContext()).apply {
            text = persona.tags.joinToString(" \u00b7 "); setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 12f; setPadding(0, dp(4), 0, 0)
        })
        header.addView(textCol)
        // Gear button
        header.addView(TextView(requireContext()).apply {
            text = "\u2699"; textSize = 18f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginStart = dp(8) }
            background = GradientDrawable().apply {
                setColor(resources.getColor(R.color.bg_secondary, null)); setStroke(1, Color.parseColor("#333355")); cornerRadius = dp(10).toFloat()
            }
            setOnClickListener {
                // TODO: open PersonaSettingsModal
            }
        })
        root.addView(header)

        // Params badge
        if (persona.modelParamsConfig != null) {
            root.addView(TextView(requireContext()).apply {
                text = "\u26A1 Auto (\u89C4\u5219)"; setTextColor(resources.getColor(R.color.accent, null)); textSize = 12f
                gravity = Gravity.CENTER; setPadding(dp(10), dp(6), dp(10), dp(6))
                background = GradientDrawable().apply { setColor(Color.parseColor("#1AE94560")); cornerRadius = dp(6).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
            })
        }

        // Tags
        val tagRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, dp(12)) }
        for (tag in persona.tags) {
            tagRow.addView(TextView(requireContext()).apply {
                text = tag; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 12f
                setPadding(dp(10), dp(4), dp(10), dp(4))
                background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_secondary, null)); cornerRadius = dp(10).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6) }
            })
        }
        root.addView(tagRow)

        // System prompt preview
        root.addView(TextView(requireContext()).apply {
            text = persona.prompt.ifEmpty { "No system prompt set" }
            setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f; lineSpacingMultiplier = 1.6f
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_secondary, null)); cornerRadius = dp(10).toFloat() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(200))
        })

        // Buttons: Back + Start Chat
        val btnRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(16), 0, 0)
        }
        btnRow.addView(TextView(requireContext()).apply {
            text = "\u8FD4\u56DE"; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 14f
            gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(10).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(10) }
            setOnClickListener { parentFragmentManager.popBackStack() }
        })
        btnRow.addView(TextView(requireContext()).apply {
            text = "\u5F00\u59CB\u5BF9\u8BDD"; setTextColor(Color.WHITE); textSize = 14f; setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.accent, null)); cornerRadius = dp(10).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                viewModel.setActivePersona(persona.id)
                // Navigate to chat tab
                parentFragmentManager.popBackStack()
            }
        })
        root.addView(btnRow)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
