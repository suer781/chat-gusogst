package com.gusogst.chat.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gusogst.chat.R
import com.gusogst.chat.model.Message
import com.gusogst.chat.model.MessageStatus
import com.gusogst.chat.model.Role

class MessageAdapter(
    private val onRegenerate: ((Message) -> Unit)? = null,
    private val onDelete: ((Message) -> Unit)? = null
) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {
    var glassEnabled = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_ASSISTANT = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position).role) {
            Role.user -> TYPE_USER
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
            is UserViewHolder -> holder.bind(msg, onDelete)
            is AssistantViewHolder -> holder.bind(msg, onRegenerate, onDelete)
        }
    }

    // ===== 用户消息 =====
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val actionBar: LinearLayout = view.findViewById(R.id.actionBar)
        private val btnCopy: TextView = view.findViewById(R.id.btnCopy)
        private val btnDelete: TextView = view.findViewById(R.id.btnDelete)

        fun bind(msg: Message, onDelete: ((Message) -> Unit)?, glass: Boolean = false) {
            MarkdownRenderer.render(msg.content, tvMessage)

            // 长按显示操作
            tvMessage.setOnLongClickListener {
                toggleActions(msg, onDelete)
                true
            }
            actionBar.visibility = View.GONE
        }

        private fun toggleActions(msg: Message, onDelete: ((Message) -> Unit)?) {
            if (actionBar.visibility == View.VISIBLE) {
                actionBar.visibility = View.GONE
                return
            }
            actionBar.visibility = View.VISIBLE
            btnCopy.setOnClickListener {
                copyToClipboard(it.context, msg.content)
                actionBar.visibility = View.GONE
            }
            btnDelete.setOnClickListener {
                onDelete?.invoke(msg)
                actionBar.visibility = View.GONE
            }
        }

        private fun copyToClipboard(context: Context, text: String) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("message", text))
            Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
        }
    }

    // ===== AI 消息 =====
    class AssistantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val thinkingBlock: LinearLayout = view.findViewById(R.id.thinkingBlock)
        private val tvThinkingContent: TextView = view.findViewById(R.id.tvThinkingContent)
        private val toolCallsContainer: LinearLayout = view.findViewById(R.id.toolCallsContainer)
        private val actionBar: LinearLayout = view.findViewById(R.id.actionBar)
        private val btnCopy: TextView = view.findViewById(R.id.btnCopy)
        private val btnRegen: TextView = view.findViewById(R.id.btnRegen)
        private val btnDelete: TextView = view.findViewById(R.id.btnDelete)

        fun bind(msg: Message, onRegen: ((Message) -> Unit)?, onDelete: ((Message) -> Unit)?, glass: Boolean = false) {
            // 正文 - Markdown 渲染
            if (msg.content.isEmpty() && msg.status == MessageStatus.streaming) {
                tvMessage.text = "..."
            } else {
                MarkdownRenderer.render(msg.content, tvMessage)
            }
            tvMessage.alpha = if (msg.status == MessageStatus.streaming) 0.8f else 1f
            tvMessage.background?.alpha = if (glass) 80 else 255

            // 思考折叠块
            if (!msg.thinking.isNullOrBlank()) {
                thinkingBlock.visibility = View.VISIBLE
                tvThinkingContent.text = msg.thinking
                tvThinkingContent.maxLines = if (msg.thinkingCollapsed) 2 else Int.MAX_VALUE
                thinkingBlock.setOnClickListener {
                    tvThinkingContent.maxLines = if (tvThinkingContent.maxLines == 2) Int.MAX_VALUE else 2
                }
            } else {
                thinkingBlock.visibility = View.GONE
            }

            // 工具调用卡片
            val tools = msg.toolCalls
            if (!tools.isNullOrEmpty()) {
                toolCallsContainer.visibility = View.VISIBLE
                toolCallsContainer.removeAllViews()
                val inflater = LayoutInflater.from(itemView.context)
                for (tool in tools) {
                    val card = inflater.inflate(R.layout.item_tool_call, toolCallsContainer, false)
                    card.findViewById<TextView>(R.id.tvToolName).text = tool.name
                    card.findViewById<TextView>(R.id.tvToolInput).text = tool.arguments
                    val statusTv = card.findViewById<TextView>(R.id.tvToolStatus)
                    val resultTv = card.findViewById<TextView>(R.id.tvToolOutput)
                    if (tool.result != null) {
                        statusTv.text = itemView.context.getString(R.string.tool_done)
                        statusTv.setTextColor(itemView.context.getColor(R.color.success))
                        resultTv.text = tool.result
                        resultTv.visibility = View.VISIBLE
                    } else {
                        statusTv.text = itemView.context.getString(R.string.tool_running)
                        statusTv.setTextColor(itemView.context.getColor(R.color.warning))
                    }
                    toolCallsContainer.addView(card)
                }
            } else {
                toolCallsContainer.visibility = View.GONE
            }

            // 操作按钮（长按显示）
            tvMessage.setOnLongClickListener {
                toggleActions(msg, onRegen, onDelete)
                true
            }
            actionBar.visibility = View.GONE
        }

        private fun toggleActions(msg: Message, onRegen: ((Message) -> Unit)?, onDelete: ((Message) -> Unit)?) {
            if (actionBar.visibility == View.VISIBLE) {
                actionBar.visibility = View.GONE
                return
            }
            actionBar.visibility = View.VISIBLE
            btnCopy.setOnClickListener {
                copyToClipboard(it.context, msg.content)
                actionBar.visibility = View.GONE
            }
            btnRegen.setOnClickListener {
                onRegen?.invoke(msg)
                actionBar.visibility = View.GONE
            }
            btnDelete.setOnClickListener {
                onDelete?.invoke(msg)
                actionBar.visibility = View.GONE
            }
        }

        private fun copyToClipboard(context: Context, text: String) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("message", text))
            Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(old: Message, new: Message) = old.id == new.id
        override fun areContentsTheSame(old: Message, new: Message) = old == new
    }
}
