package com.gusogst.chat.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.gusogst.chat.R
import com.gusogst.chat.data.settings.ChatSettingsManager
import com.gusogst.chat.ui.MainActivity

/**
 * 主题设置页面 — 深色模式、主题色选择、强调色
 */
class ThemeSettingsFragment : Fragment() {

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
            setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
        root.addView(backBtn)

        // ===== 标题 =====
        root.addSectionTitle("外观设置")

        // ===== 深色模式 =====
        root.addSectionTitle("深色模式")

        val modes = listOf(
            "light" to "浅色模式",
            "dark" to "深色模式",
            "pureWhite" to "纯白模式",
            "pureBlack" to "纯黑模式 (AMOLED)",
            "system" to "跟随系统"
        )

        val radioGroup = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(0, 0, 0, 24)
        }

        val currentMode = settingsManager.getThemeMode()
        modes.forEach { (mode, label) ->
            val rb = RadioButton(requireContext()).apply {
                text = label
                textSize = 15f
                id = View.generateViewId()
                isChecked = mode == currentMode
                setPadding(16, 16, 16, 16)
            }
            radioGroup.addView(rb)

            rb.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    settingsManager.setThemeMode(mode)
                    // 重启 Activity 并应用动画
                    (activity as? MainActivity)?.applyThemeWithAnimation()
                }
            }
        }
        root.addView(radioGroup)

        root.addDivider()

        // ===== 主题色选择 =====
        root.addSectionTitle("主题色")

        val colors = listOf(
            "Purple (默认)" to "#6200EE",
            "Blue" to "#2196F3",
            "Teal" to "#009688",
            "Green" to "#4CAF50",
            "Orange" to "#FF9800",
            "Red" to "#F44336",
            "Pink" to "#E91E63",
            "Indigo" to "#3F51B5"
        )

        val colorGrid = GridLayout(requireContext()).apply {
            columnCount = 4
            rowCount = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }

        val currentColor = settingsManager.getThemeColor()

        colors.forEach { (name, hex) ->
            val colorView = View(requireContext()).apply {
                val size = 120
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(16, 16, 16, 16)
                }
                setBackgroundColor(android.graphics.Color.parseColor(hex))

                // 选中指示
                if (hex == currentColor) {
                    foreground = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setStroke(4, android.graphics.Color.WHITE)
                    }
                }

                setOnClickListener {
                    settingsManager.setThemeColor(hex)
                    Toast.makeText(context, "主题色已切换: $name", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.applyThemeWithAnimation()
                }
            }
            colorGrid.addView(colorView)
        }
        root.addView(colorGrid)

        root.addDivider()

        // ===== 强调色 =====
        root.addSectionTitle("强调色")
        root.addInfoItem("用于按钮、链接、高亮等交互元素", "")

        val accentColors = listOf(
            "Amber" to "#FFC107",
            "Cyan" to "#00BCD4",
            "Lime" to "#CDDC39",
            "Deep Orange" to "#FF5722",
            "Light Blue" to "#03A9F4",
            "Deep Purple" to "#673AB7"
        )

        val accentGrid = GridLayout(requireContext()).apply {
            columnCount = 6
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }

        val currentAccent = settingsManager.getAccentColor()

        accentColors.forEach { (name, hex) ->
            val colorView = View(requireContext()).apply {
                val size = 96
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(android.graphics.Color.parseColor(hex))

                if (hex == currentAccent) {
                    foreground = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setStroke(4, android.graphics.Color.WHITE)
                    }
                }

                setOnClickListener {
                    settingsManager.setAccentColor(hex)
                    Toast.makeText(context, "强调色已切换: $name", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.applyThemeWithAnimation()
                }
            }
            accentGrid.addView(colorView)
        }
        root.addView(accentGrid)

        root.addDivider()

        // ===== 字体大小 =====
        root.addSectionTitle("字体大小")

        val fontSizes = listOf(
            "小" to 0.85f,
            "标准" to 1.0f,
            "大" to 1.15f,
            "特大" to 1.3f
        )
        val currentFontSize = settingsManager.getFontSize()

        val fontGroup = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(0, 0, 0, 24)
        }

        fontSizes.forEach { (label, scale) ->
            val rb = RadioButton(requireContext()).apply {
                text = label
                textSize = 14f
                id = View.generateViewId()
                isChecked = Math.abs(scale - currentFontSize) < 0.01f
                setPadding(16, 16, 16, 16)
            }
            fontGroup.addView(rb)

            rb.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    settingsManager.setFontSize(scale)
                    (activity as? MainActivity)?.recreate()
                }
            }
        }
        root.addView(fontGroup)

        scrollView.addView(root)
        return scrollView
    }

    // ===== 扩展函数 =====

    private fun LinearLayout.addSectionTitle(title: String) {
        addView(TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, 24, 0, 8)
        })
    }

    private fun LinearLayout.addInfoItem(title: String, value: String) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }
        container.addView(TextView(requireContext()).apply {
            text = title
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (value.isNotEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = value
                textSize = 14f
                setTextColor(0xFF888888.toInt())
            })
        }
        addView(container)
    }

    private fun LinearLayout.addDivider() {
        addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply { setMargins(0, 16, 0, 16) }
            setBackgroundColor(0xFF333333.toInt())
        })
    }
}
