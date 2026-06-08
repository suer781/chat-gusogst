package com.gusogst.chat.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.gusogst.chat.R
import com.gusogst.chat.data.settings.ChatSettingsManager
import com.gusogst.chat.ui.MainActivity
import com.gusogst.chat.ui.theme.ThemeController
import com.gusogst.chat.util.RealHdrHelper

/**
 * 主题设置页面 - 纯本地函数方式，不依赖 HTTP
 * 像手机设置一样流畅的主题切换
 */
class ThemeSettingsFragment : Fragment() {

    private lateinit var themeController: ThemeController
    private lateinit var settingsManager: ChatSettingsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 使用 ThemeController 管理主题
        themeController = ThemeController.getInstance(requireContext())
        settingsManager = ChatSettingsManager(requireContext())

        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(48), dp(48), dp(48))
        }

        // 返回按钮
        val backBtn = TextView(requireContext()).apply {
            text = "← 返回"
            textSize = 14f
            setTextColor(themeController.getAccentColorInt())
            setPadding(0, 0, 0, dp(32))
            setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
        root.addView(backBtn)

        // 标题
        root.addSectionTitle("主题设置")

        // 深色模式选择
        root.addThemeModeSelector()

        root.addDivider()

        // 主题色选择
        root.addSectionTitle("主题色")
        root.addThemeColorSelector()

        root.addDivider()

        // 强调色选择
        root.addSectionTitle("强调色")
        root.addAccentColorSelector()

        root.addDivider()

        // 字体大小
        root.addSectionTitle("字体大小")
        root.addFontSizeSelector()

        // HDR设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && RealHdrHelper.isHdrSupported(requireContext())) {
            root.addDivider()
            root.addHdrSettings()
        }

        scrollView.addView(root)
        return scrollView
    }

    private fun ViewGroup.addThemeModeSelector() {
        val modes = listOf(
            ThemeController.MODE_LIGHT to "浅色模式",
            ThemeController.MODE_DARK to "深色模式",
            ThemeController.MODE_PURE_WHITE to "纯白模式",
            ThemeController.MODE_PURE_BLACK to "纯黑 (AMOLED)",
            ThemeController.MODE_SYSTEM to "跟随系统"
        )

        val radioGroup = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }

        val currentMode = themeController.getThemeMode()

        modes.forEach { (mode, label) ->
            val rb = RadioButton(requireContext()).apply {
                text = label
                textSize = 15f
                id = View.generateViewId()
                isChecked = mode == currentMode
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
            radioGroup.addView(rb)

            rb.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    // 纯本地函数调用，无 HTTP
                    (activity as? MainActivity)?.applyThemeWithAnimation(mode)
                }
            }
        }
        addView(radioGroup)
    }

    private fun ViewGroup.addThemeColorSelector() {
        val colors = listOf(
            "#6200EE" to "紫色",
            "#2196F3" to "蓝色",
            "#009688" to "青色",
            "#4CAF50" to "绿色",
            "#FF9800" to "橙色",
            "#F44336" to "红色",
            "#E91E63" to "粉色",
            "#3F51B5" to "靛蓝"
        )

        val grid = GridLayout(requireContext()).apply {
            columnCount = 4
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(24)) }
        }

        val currentColor = themeController.getThemeColor()

        colors.forEach { (hex, name) ->
            val colorView = ColorSwatchView(requireContext()).apply {
                setColor(hex)
                isSelected = hex == currentColor
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dp(60)
                    height = dp(60)
                    setMargins(dp(8), dp(8), dp(8), dp(8))
                }
                setOnClickListener {
                    // 纯本地函数调用
                    themeController.setThemeColor(hex)
                    (activity as? MainActivity)?.applyThemeWithAnimation(
                        themeController.getThemeMode()
                    )
                }
            }
            grid.addView(colorView)
        }
        addView(grid)
    }

    private fun ViewGroup.addAccentColorSelector() {
        val colors = listOf(
            "#FFC107" to "琥珀",
            "#00BCD4" to "青色",
            "#CDDC39" to "青柠",
            "#FF5722" to "深橙",
            "#03A9F4" to "浅蓝",
            "#673AB7" to "深紫"
        )

        val grid = GridLayout(requireContext()).apply {
            columnCount = 6
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(24)) }
        }

        val currentAccent = themeController.getAccentColor()

        colors.forEach { (hex, name) ->
            val colorView = ColorSwatchView(requireContext()).apply {
                setColor(hex)
                isSelected = hex == currentAccent
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dp(40)
                    height = dp(40)
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }
                setOnClickListener {
                    themeController.setAccentColor(hex)
                    (activity as? MainActivity)?.quickSwitchTheme(
                        themeController.getThemeMode()
                    )
                }
            }
            grid.addView(colorView)
        }
        addView(grid)
    }

    private fun ViewGroup.addFontSizeSelector() {
        val sizes = listOf(
            0.85f to "小",
            1.0f to "标准",
            1.15f to "大",
            1.3f to "特大"
        )

        val radioGroup = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(0, 0, 0, dp(24))
        }

        val currentSize = themeController.getFontSize()

        sizes.forEach { (scale, label) ->
            val rb = RadioButton(requireContext()).apply {
                text = label
                textSize = 14f
                id = View.generateViewId()
                isChecked = kotlin.math.abs(scale - currentSize) < 0.01f
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
            radioGroup.addView(rb)

            rb.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    themeController.setFontSize(scale)
                    activity?.recreate()
                }
            }
        }
        addView(radioGroup)
    }

    private fun ViewGroup.addHdrSettings() {
        val hdrSection = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24), 0, dp(16))
        }

        // HDR标题
        hdrSection.addView(TextView(requireContext()).apply {
            text = "HDR 显示"
            textSize = 16f
            setTextColor(themeController.getSecondaryTextColor())
        })

        // HDR状态
        val hdrStatus = TextView(requireContext()).apply {
            text = RealHdrHelper.getHdrStatusDescription(requireContext())
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(0, dp(4), 0, dp(16))
        }
        hdrSection.addView(hdrStatus)

        // HDR开关
        hdrSection.addSwitchItem(
            "启用 HDR",
            "开启后，当显示HDR内容时，系统会自动切换到HDR模式（需要应用重启）",
            settingsManager.isHdrEnabled()
        ) { enabled ->
            settingsManager.setHdrEnabled(enabled)
            // 通知MainActivity刷新HDR设置
            (activity as? MainActivity)?.refreshHdrSettings()
            Toast.makeText(
                requireContext(),
                if (enabled) "HDR已启用（需要重启应用以生效）" else "HDR已禁用",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 自动HDR开关
        hdrSection.addSwitchItem(
            "自动 HDR",
            "智能检测内容并自动切换HDR模式（推荐）",
            settingsManager.isAutoHdrEnabled()
        ) { enabled ->
            settingsManager.setAutoHdrEnabled(enabled)
        }

        addView(hdrSection)
    }

    private fun LinearLayout.addSectionTitle(title: String) {
        addView(TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTextColor(themeController.getSecondaryTextColor())
            setPadding(0, dp(24), 0, dp(8))
        })
    }

    private fun LinearLayout.addDivider() {
        addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply { setMargins(0, dp(16), 0, dp(16)) }
            setBackgroundColor(
                if (themeController.isDarkTheme()) {
                    android.graphics.Color.parseColor("#2A2A40")
                } else {
                    android.graphics.Color.parseColor("#E0E0E0")
                }
            )
        })
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /**
     * 颜色选择视图 - 自定义简单的圆形颜色块
     */
    inner class ColorSwatchView(context: android.content.Context) : View(context) {
        private var colorHex: String = "#000000"
        private var selected: Boolean = false

        fun setColor(hex: String) {
            colorHex = hex
            invalidate()
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            
            // 绘制圆形背景
            paint.color = android.graphics.Color.parseColor(colorHex)
            canvas.drawCircle(width / 2f, height / 2f, kotlin.math.min(width, height) / 2f - 4, paint)
            
            // 如果选中，绘制边框
            if (selected) {
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = android.graphics.Color.WHITE
                canvas.drawCircle(width / 2f, height / 2f, kotlin.math.min(width, height) / 2f - 6, paint)
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val size = kotlin.math.min(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec)
            )
            setMeasuredDimension(size, size)
        }
    }
}
