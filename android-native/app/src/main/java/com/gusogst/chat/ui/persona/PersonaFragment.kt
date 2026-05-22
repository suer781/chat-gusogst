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
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaFragment : Fragment() {

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var personaList: LinearLayout
    private lateinit var etSearch: EditText

    private var allPersonas: List<Persona> = emptyList()
    private var activePersonaId: String? = null
    private var searchQuery: String = ""

    // Preset personas matching web version
    data class PresetPersona(
        val id: String,
        val name: String,
        val emoji: String,
        val description: String,
        val tags: List<String>
    )

    private val presets = listOf(
        PresetPersona("default", "Hermes", "🌿", "Calm, warm, and analytical — your everyday companion", listOf("general")),
        PresetPersona("creative", "Muse", "🎨", "Creative and curious, perfect for brainstorming", listOf("creative", "writing")),
        PresetPersona("coder", "Hephaestus", "⚒️", "Precise and technical, your coding partner", listOf("coding", "technical")),
        PresetPersona("analyst", "Athena", "🦉", "Analytical and strategic, deep thinking partner", listOf("analysis", "strategy")),
        PresetPersona("tutor", "Socrates", "📚", "Curious and warm, great for learning", listOf("education", "learning")),
        PresetPersona("friend", "Companion", "💛", "Warm and playful, your casual friend", listOf("casual", "support"))
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

        viewModel.personas.observe(viewLifecycleOwner) { personas ->
            allPersonas = personas
            buildCards()
        }
        viewModel.activeConversation.observe(viewLifecycleOwner) { conv ->
            activePersonaId = conv?.personaId
            buildCards()
        }
    }

    private fun buildCards() {
        personaList.removeAllViews()

        // Merge presets with custom personas
        val displayPersonas = mutableListOf<Pair<String, Triple<String, String, List<String>>>>() // id, (name, emoji/desc, tags)
        for (p in presets) {
            val fullText = "${p.name} ${p.emoji} ${p.tags.joinToString(" ")} ${p.description}".lowercase()
            if (searchQuery.isEmpty() || fullText.contains(searchQuery)) {
                displayPersonas.add(p.id to Triple(p.name, "${p.emoji}|${p.description}", p.tags))
            }
        }
        // Add custom personas from ViewModel
        for (persona in allPersonas) {
            if (persona.id.startsWith("custom-")) {
                val tags = persona.tags ?: emptyList()
                val fullText = "${persona.name} ${tags.joinToString(" ")} ${persona.prompt ?: ""}".lowercase()
                if (searchQuery.isEmpty() || fullText.contains(searchQuery)) {
                    displayPersonas.add(persona.id to Triple(persona.name, "${persona.avatar ?: "🎭"}|Custom persona", tags))
                }
            }
        }

        for ((id, info) in displayPersonas) {
            addPersonaCard(id, info.first, info.second, info.third)
        }

        // Create new persona button
        addCreateButton()
    }

    private fun addPersonaCard(id: String, name: String, emojiAndDesc: String, tags: List<String>) {
        val parts = emojiAndDesc.split("|", limit = 2)
        val emoji = parts.getOrElse(0) { "🌿" }
        val desc = parts.getOrElse(1) { "" }
        val isSelected = id == activePersonaId

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val bg = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (isSelected) Color.parseColor("#1AE94560") else Color.parseColor("#1A1A3A"))
                setStroke(1, if (isSelected) Color.parseColor("#E94560") else Color.parseColor("#2A2A4A"))
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            isClickable = true
            isFocusable = true
        }

        // Avatar circle
        val avatarSize = dp(48)
        val avatarBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E94560"))
        }
        val avatarTv = TextView(requireContext()).apply {
            text = emoji
            textSize = 22f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(avatarSize, avatarSize)
        }
        val avatarFrame = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize)
            background = avatarBg
            addView(avatarTv)
        }

        // Text info
        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            }
        }

        val nameTv = TextView(requireContext()).apply {
            text = name
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }
        textLayout.addView(nameTv)

        if (desc.isNotEmpty()) {
            val descTv = TextView(requireContext()).apply {
                text = desc
                setTextColor(Color.parseColor("#A0A0B8"))
                textSize = 13f
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) }
            }
            textLayout.addView(descTv)
        }

        // Tag chips
        if (tags.isNotEmpty()) {
            val tagRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
            }
            for (tag in tags.take(3)) {
                val chip = TextView(requireContext()).apply {
                    text = tag
                    setTextColor(Color.parseColor("#8888A0"))
                    textSize = 11f
                    setPadding(dp(8), dp(2), dp(8), dp(2))
                    val chipBg = GradientDrawable().apply {
                        cornerRadius = dp(100).toFloat()
                        setColor(Color.parseColor("#22224A"))
                    }
                    background = chipBg
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = dp(4) }
                }
                tagRow.addView(chip)
            }
            textLayout.addView(tagRow)
        }

        card.addView(avatarFrame)
        card.addView(textLayout)

        // Selected indicator
        if (isSelected) {
            val checkTv = TextView(requireContext()).apply {
                text = "✓"
                setTextColor(Color.parseColor("#E94560"))
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            }
            card.addView(checkTv)
        }

        card.setOnClickListener {
            // Find or create persona in ViewModel
            val preset = presets.find { it.id == id }
            if (preset != null) {
                val persona = Persona(
                    id = preset.id,
                    name = preset.name,
                    avatar = preset.emoji,
                    prompt = "You are ${preset.name}. ${preset.description}",
                    tags = preset.tags
                )
                viewModel.setActivePersona(persona.id)
            } else {
                // Custom persona - already in ViewModel
                allPersonas.find { it.id == id }?.let { viewModel.setActivePersona(it) }
            }
        }

        personaList.addView(card)
    }

    private fun addCreateButton() {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val bg = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(1, Color.parseColor("#404060"))
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        }

        val iconTv = TextView(requireContext()).apply {
            text = "＋"
            setTextColor(Color.parseColor("#E94560"))
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            val circleBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1AE94560"))
            }
            background = circleBg
        }

        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            }
        }
        textLayout.addView(TextView(requireContext()).apply {
            text = "创建角色"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        })
        textLayout.addView(TextView(requireContext()).apply {
            text = "自定义你的AI伙伴"
            setTextColor(Color.parseColor("#A0A0B8"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })

        val arrowTv = TextView(requireContext()).apply {
            text = ">"
            setTextColor(Color.parseColor("#8888A0"))
            textSize = 18f
        }

        card.addView(iconTv)
        card.addView(textLayout)
        card.addView(arrowTv)

        card.setOnClickListener {
            val newPersona = Persona(
                id = "custom-${System.currentTimeMillis()}",
                name = "Custom",
                avatar = "🎭",
                prompt = "You are a helpful assistant.",
                tags = listOf("custom")
            )
            viewModel.setActivePersona(newPersona)
        }

        personaList.addView(card)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}