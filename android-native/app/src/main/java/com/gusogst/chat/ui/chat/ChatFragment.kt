package com.gusogst.chat.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gusogst.chat.R
import com.gusogst.chat.model.Message
import com.gusogst.chat.model.Role

class ChatFragment : Fragment() {
    private lateinit var rvMessages: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvMessages = view.findViewById(R.id.rvMessages)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        etInput = view.findViewById(R.id.etInput)
        btnSend = view.findViewById(R.id.btnSend)

        adapter = MessageAdapter()
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvMessages.adapter = adapter
        btnSend.setOnClickListener { sendMessage() }
        updateEmptyState()
    }

    private fun sendMessage() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) return
        messages.add(Message(role = Role.USER, content = text))
        etInput.text.clear()
        // TODO: 接入 AI API
        messages.add(Message(role = Role.ASSISTANT, content = "(AI 接口尚未接入)"))
        adapter.submitList(messages.toList())
        rvMessages.scrollToPosition(messages.size - 1)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
    }
}
