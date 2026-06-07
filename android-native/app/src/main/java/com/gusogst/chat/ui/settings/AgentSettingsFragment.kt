package com.gusogst.chat.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.gusogst.chat.R
import com.gusogst.chat.data.settings.ChatSettingsManager

/**
 * Agent 设置页面 — 角色名称、提示词、语气、回复长度
 */
class AgentSettingsFragment : Fragment() {

    private lateinit var settingsManager: ChatSettingsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsManager = ChatSettingsManager(requireContext())

        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // ===== 返回按钮 =====
        val backBtn = TextView(requireContext()).apply {
            text = "← 返回通用设置"
            textSize = 14f
            setTextColor(0xFF7C4DFF.toInt())
            setPadding(0, 0, 0, 32)
            setOnClickListener { parentFragmentManager.popBackStack() }
        }
        root.addView(backBtn)

        root.addView(TextView(requireContext()).apply {
            text = "Agent 设置"
            textSize = 22f
            setPadding(0, 0, 0, 32)
        })

        // ===== 角色名称 =====
        root.addSectionTitle("角色名称")
        val nameInput = EditText(requireContext()).apply {
            setText(settingsManager.getAgentName())
            hint = "例如：小助手、AI管家"
            setPadding(16, 16, 16, 16)
        }
        root.addView(nameInput)

        root.addDivider()

        // ===== 系统提示词 =====
        root.addSectionTitle("系统提示词")
        root.addView(TextView(requireContext()).apply {
            text = "定义 AI 的角色和行为方式"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, 8)
        })
        val promptInput = EditText(requireContext()).apply {
            setText(settingsManager.getSystemPrompt())
            hint = "你是一个有帮助的AI助手..."
            minLines = 3
            maxLines = 8
            setPadding(16, 16, 16, 16)
            gravity = android.view.Gravity.TOP
        }
        root.addView(promptInput)

        root.addDivider()

        // ===== 语气风格 =====
        root.addSectionTitle("语气风格")
        val tones = listOf(
            "default" to "默认",
            "formal" to "正式",
            "casual" to "随意",
            "friendly" to "友好",
            "professional" to "专业",
            "humorous" to "幽默"
        )
        val currentTone = settingsManager.getAgentTone()
        val toneSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                tones.map { it.second }
            )
            setSelection(tones.indexOfFirst { it.first == currentTone }.coerceAtLeast(0))
        }
        toneSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                settingsManager.setAgentTone(tones[pos].first)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        root.addView(toneSpinner)

        root.addDivider()

        // ===== 回复长度 =====
        root.addSectionTitle("回复长度")
        val lengths = listOf(
            "short" to "简短（1-2句）",
            "medium" to "适中（3-5句）",
            "long" to "详细（完整回答）",
            "auto" to "自动判断"
        )
        val currentLength = settingsManager.getAgentResponseLength()
        val lengthSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                lengths.map { it.second }
            )
            setSelection(lengths.indexOfFirst { it.first == currentLength }.coerceAtLeast(0))
        }
        lengthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                settingsManager.setAgentResponseLength(lengths[pos].first)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        root.addView(lengthSpinner)

        root.addDivider()

        // ===== Temperature =====
        root.addSectionTitle("创造力 (Temperature)")
        root.addView(TextView(requireContext()).apply {
            text = "低=精确保守，高=创意发散"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, 8)
        })

        val tempLabel = TextView(requireContext()).apply {
            text = String.format("%.2f", settingsManager.getTemperature())
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }
        val tempSeekBar = SeekBar(requireContext()).apply {
            max = 100
            progress = (settingsManager.getTemperature() * 100).toInt()
        }
        tempSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val temp = progress.toFloat() / 100f
                tempLabel.text = String.format("%.2f", temp)
                settingsManager.setTemperature(temp)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(tempSeekBar)
        root.addView(tempLabel)

        root.addDivider()

        // ===== Top P =====
        root.addSectionTitle("Top P")
        val topPLabel = TextView(requireContext()).apply {
            text = String.format("%.2f", settingsManager.getTopP())
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }
        val topPSeekBar = SeekBar(requireContext()).apply {
            max = 100
            progress = (settingsManager.getTopP() * 100).toInt()
        }
        topPSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val topP = progress.toFloat() / 100f
                topPLabel.text = String.format("%.2f", topP)
                settingsManager.setTopP(topP)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(topPSeekBar)
        root.addView(topPLabel)

        root.addDivider()

        // ===== 保存按钮 =====
        val saveBtn = Button(requireContext()).apply {
            text = "保存设置"
            setOnClickListener {
                settingsManager.setAgentName(nameInput.text.toString())
                settingsManager.setSystemPrompt(promptInput.text.toString())
                Toast.makeText(context, "Agent 设置已保存", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(saveBtn)

        scrollView.addView(root)
        return scrollView
    }

    private fun LinearLayout.addSectionTitle(title: String) {
        addView(TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, 24, 0, 8)
        })
    }

    private fun LinearLayout.addDivider() {
        addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(0, 16, 0, 16) }
            setBackgroundColor(0xFF333333.toInt())
        })
    }
}
