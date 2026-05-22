package com.gusogst.chat.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.android.material.textview.MaterialTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gusogst.chat.R
import com.gusogst.chat.viewmodel.ChatViewModel

class ChatFragment : Fragment() {

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var rvMessages: RecyclerView
    private lateinit var tvEmpty: LinearLayout
    private lateinit var etInput: EditText
    private lateinit var btnSend: MaterialTextView
    private lateinit var adapter: MessageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvMessages = view.findViewById(R.id.rvMessages)
        tvEmpty = view.findViewById(R.id.emptyState)
        etInput = view.findViewById(R.id.etInput)
        btnSend = view.findViewById(R.id.btnSend)

        adapter = MessageAdapter(
            onRegenerate = { msg -> viewModel.regenerate(msg) },
            onDelete = { msg -> viewModel.deleteMessage(msg.id) }
        )
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        btnSend.setOnClickListener { sendMessage() }

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            adapter.submitList(msgs)
            if (msgs.isNotEmpty()) rvMessages.scrollToPosition(msgs.size - 1)
            tvEmpty.visibility = if (msgs.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isStreaming.observe(viewLifecycleOwner) { streaming ->
            btnSend.isEnabled = !streaming
            etInput.isEnabled = !streaming
        }

        viewModel.settings.observe(viewLifecycleOwner) { s ->
            adapter.glassEnabled = s.glassEnabled
        }
    }

    private fun sendMessage() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) return
        etInput.text.clear()
        viewModel.sendMessage(text)
    }
}
