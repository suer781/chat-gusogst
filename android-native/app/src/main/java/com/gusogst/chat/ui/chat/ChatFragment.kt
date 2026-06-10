package com.gusogst.chat.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import com.google.android.material.textview.MaterialTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.content.res.Configuration
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gusogst.chat.R
import com.gusogst.chat.model.MessageStatus
import com.gusogst.chat.ui.persona.PersonaProfileFragment
import com.gusogst.chat.viewmodel.ChatViewModel
import com.gusogst.chat.util.HdrHelper
import com.gusogst.chat.util.MaterialAnimator
import com.gusogst.chat.util.HapticsHelper

class ChatFragment : Fragment() {

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var rvMessages: RecyclerView
    private lateinit var tvEmpty: LinearLayout
    private lateinit var etInput: EditText
    private lateinit var btnSend: MaterialTextView
    private lateinit var tvPersonaAvatar: TextView
    private lateinit var tvPersonaName: TextView
    private lateinit var tvTypingStatus: TextView
    private lateinit var btnNewChat: ImageButton
    private lateinit var adapter: MessageAdapter
    private lateinit var haptics: HapticsHelper

    companion object {
        private const val EXIT_DURATION_MS = 80L
    }

    // Grok 风格流式震动：记录上一个消息长度，检测增量
    private var _lastStreamingLength = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvMessages = view.findViewById(R.id.rvMessages)
        tvEmpty = view.findViewById(R.id.emptyState)
        etInput = view.findViewById(R.id.etInput)
        btnSend = view.findViewById(R.id.btnSend)
        tvPersonaAvatar = view.findViewById(R.id.tvPersonaAvatar)
        tvPersonaName = view.findViewById(R.id.tvPersonaName)
        tvTypingStatus = view.findViewById(R.id.tvTypingStatus)
        btnNewChat = view.findViewById(R.id.btnNewChat)
        haptics = HapticsHelper(requireContext())

        adapter = MessageAdapter(
            onRegenerate = { msg -> viewModel.regenerate(msg) },
            onDelete = { msg -> viewModel.deleteMessage(msg.id) }
        )
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        btnSend.setOnClickListener { sendMessage() }
        // Fix 6: Apply button effects (ripple + press animation) + HDR glow via observer below
        MaterialAnimator.applyButtonEffects(btnSend)

        // Fix 7: Input focus listener for dynamic HDR glow
        etInput.setOnFocusChangeListener { _, hasFocus ->
            val isHdr = viewModel.settings.value?.hdrEnabled ?: false
            val isDark = when (viewModel.settings.value?.theme) {
                "dark", "pureBlack" -> true; "light", "pureWhite" -> false
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            HdrHelper.applyInputGlow(etInput, isHdr, hasFocus, isDark)
        }

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            adapter.submitList(msgs)
            if (msgs.isNotEmpty()) rvMessages.scrollToPosition(msgs.size - 1)
            tvEmpty.visibility = if (msgs.isEmpty()) View.VISIBLE else View.GONE

            // Grok 风格：流式输出时每新增内容触发微震动
            if (viewModel.isStreaming.value == true && msgs.isNotEmpty()) {
                val lastMsg = msgs.last()
                if (lastMsg.status == MessageStatus.STREAMING && lastMsg.content.length > _lastStreamingLength) {
                    haptics.microTick()
                }
                _lastStreamingLength = lastMsg.content.length
            } else {
                _lastStreamingLength = 0
            }
        }

        viewModel.isStreaming.observe(viewLifecycleOwner) { streaming ->
            btnSend.isEnabled = !streaming
            etInput.isEnabled = !streaming
            if (!streaming) _lastStreamingLength = 0
        }

        viewModel.settings.observe(viewLifecycleOwner) { s ->
            val isDark = when (s.theme) {
                "dark", "pureBlack" -> true
                "light", "pureWhite" -> false
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            adapter.glassEnabled = s.glassEnabled
            adapter.hdrEnabled = s.hdrEnabled
            adapter.isDark = isDark
            adapter.fontSize = try { s.fontSize.toInt() } catch (_: Exception) { 14 }

            // Fix 6: Apply HDR button glow to send button
            HdrHelper.applyButtonGlow(btnSend, s.hdrEnabled, isDark)

            // Fix 7: Apply HDR input glow to edit text (check current focus)
            HdrHelper.applyInputGlow(etInput, s.hdrEnabled, etInput.hasFocus(), isDark)
        }

        // 角色信息与打字状态
        viewModel.personas.observe(viewLifecycleOwner) { personas ->
            val activeId = viewModel.activeConversation.value?.personaId
            val persona = personas.find { it.id == activeId }
            persona?.let {
                tvPersonaAvatar.text = it.emoji ?: it.name.firstOrNull()?.uppercase() ?: "✦"
                tvPersonaName.text = it.name
                adapter.setPersonaInfo(it.name, it.emoji)
            }
        }

        // 打字状态：只在流式时显示轻量提示
        viewModel.typingState.observe(viewLifecycleOwner) { state ->
            tvTypingStatus.text = when (state) {
                ChatViewModel.TypingState.THINKING -> "思考中…"
                ChatViewModel.TypingState.TYPING -> "正在回复…"
                else -> ""
            }
        }

        // 头像点击：跳转到角色资料页
        tvPersonaAvatar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PersonaProfileFragment())
                .addToBackStack(null)
                .commit()
        }
        btnNewChat.setOnClickListener {
            etInput.text.clear()
            viewModel.createConversation()
        }

        MaterialAnimator.viewEnter(view)
    }

    private fun sendMessage() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) return
        etInput.text.clear()
        haptics.sendPulse()
        viewModel.sendMessage(text)
    }
}