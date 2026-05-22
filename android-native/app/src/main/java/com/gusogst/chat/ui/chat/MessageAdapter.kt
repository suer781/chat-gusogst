package com.gusogst.chat.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gusogst.chat.R
import com.gusogst.chat.model.Message
import com.gusogst.chat.model.Role

class MessageAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {
    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_ASSISTANT = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position).role) {
            Role.USER -> TYPE_USER
            else -> TYPE_ASSISTANT
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            else -> AssistantViewHolder(inflater.inflate(R.layout.item_message_assistant, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(msg)
            is AssistantViewHolder -> holder.bind(msg)
        }
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        fun bind(msg: Message) { tvMessage.text = msg.content }
    }

    class AssistantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val thinkingBlock: LinearLayout = view.findViewById(R.id.thinkingBlock)
        private val tvThinkingContent: TextView = view.findViewById(R.id.tvThinkingContent)

        fun bind(msg: Message) {
            tvMessage.text = msg.content
            if (!msg.thinking.isNullOrBlank()) {
                thinkingBlock.visibility = View.VISIBLE
                tvThinkingContent.text = msg.thinking
            } else {
                thinkingBlock.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(old: Message, new: Message) = old.id == new.id
        override fun areContentsTheSame(old: Message, new: Message) = old == new
    }
}
