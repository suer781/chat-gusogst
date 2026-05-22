package com.gusogst.chat.ui.persona

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.Persona
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var container: LinearLayout
    private var activePersonaId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_persona, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.personaContainer)
        viewModel.personas.observe(viewLifecycleOwner) { buildCards(it) }
        viewModel.activeConversation.observe(viewLifecycleOwner) {
            activePersonaId = it?.personaId
            viewModel.personas.value?.let { p -> buildCards(p) }
        }
    }

    private fun buildCards(personas: List<Persona>) {
        container.removeAllViews()
        val title = TextView(requireContext()).apply {
            text = "选择角色"
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }
        container.addView(title)
        addCard(Persona(id = "", name = "默认", avatar = "❤", prompt = ""))
        for (persona in personas) addCard(persona)
    }

    private fun addCard(persona: Persona) {
        val isActive = persona.id == activePersonaId || (persona.id == "" && activePersonaId == null)
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(R.drawable.bg_bubble_assistant)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(16), dp(4), dp(16), dp(4)) }
        }
        val avatarBg = try { Color.parseColor(persona.bgColor.ifEmpty { "#E94560" }) }
            catch (_: Exception) { Color.parseColor("#E94560") }
        val avatar = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(avatarBg)
            }
        }
        val emoji = TextView(requireContext()).apply {
            text = persona.avatar.ifEmpty { "❤" }
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        avatar.addView(emoji)
        val name = TextView(requireContext()).apply {
            text = persona.name.ifEmpty { "默认" }
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val check = TextView(requireContext()).apply {
            text = if (isActive) "✓" else ""
            setTextColor(resources.getColor(R.color.accent, null))
            textSize = 18f
        }
        card.addView(avatar)
        card.addView(name)
        card.addView(check)
        card.setOnClickListener { viewModel.setActivePersona(persona.id.ifEmpty { null }) }
        container.addView(card)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
