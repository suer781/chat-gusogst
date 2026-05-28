package com.gusogst.chat.ui.persona

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.Persona
import com.gusogst.chat.model.PersonalityTraits
import com.gusogst.chat.viewmodel.ChatViewModel

class PersonaProfileFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private var persona: Persona? = null

    companion object {
        private const val ARG_PERSONA = "persona"
        fun newInstance(p: Persona) = PersonaProfileFragment().apply {
            arguments = Bundle().apply { putSerializable(ARG_PERSONA, p) }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        persona = arguments?.getSerializable(ARG_PERSONA) as? Persona
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val root = (view as ScrollView).getChildAt(0) as LinearLayout
        buildUI(root)
    }

    @Suppress("DEPRECATION")
    private fun buildUI(root: LinearLayout) {
        root.removeAllViews()
        val p = persona ?: return

        // ========== Header：头像 + 名称 + 齿轮 ==========
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(16))
        }
        val avatarBg = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            colors = intArrayOf(resources.getColor(R.color.accent, null), resources.getColor(R.color.accent_hover, null))
            orientation = GradientDrawable.Orientation.TL_BR
        }
        header.addView(FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply { marginEnd = dp(12) }
            background = avatarBg
            addView(TextView(requireContext()).apply {
                text = p.avatar.ifEmpty { p.name.first().toString() }; textSize = 28f
                gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            })
        })
        val textCol = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(requireContext()).apply {
            text = p.name; setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 20f; setTypeface(null, Typeface.BOLD)
        })
        textCol.addView(TextView(requireContext()).apply {
            text = p.tags.joinToString(" · "); setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 12f; setPadding(0, dp(4), 0, 0)
        })
        header.addView(textCol)
        // 齿轮设置按钮
        header.addView(TextView(requireContext()).apply {
            text = "\u2699"; textSize = 18f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginStart = dp(8) }
            background = GradientDrawable().apply {
                setColor(resources.getColor(R.color.bg_secondary, null))
                setStroke(1, Color.parseColor("#333355")); cornerRadius = dp(10).toFloat()
            }
            setOnClickListener { showSettingsDialog(p) }
        })
        root.addView(header)

        // ========== 人格特质条 ==========
        root.addView(TextView(requireContext()).apply {
            text = "人格特质"; setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f; setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(8) }
        })
        val traits = listOf(
            "冷静" to p.personality.calm, "温暖" to p.personality.warm,
            "分析" to p.personality.analytical, "创造" to p.personality.creative,
            "好奇" to p.personality.curious, "精准" to p.personality.precise,
            "风趣" to p.personality.playful, "活力" to p.personality.energetic
        )
        for ((name, value) in traits) {
            val traitRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(6) }
            }
            // 标签
            traitRow.addView(TextView(requireContext()).apply {
                text = name; setTextColor(Color.parseColor("#A0A0B8")); textSize = 13f
                layoutParams = LinearLayout.LayoutParams(dp(50), WRAP_CONTENT)
            })
            // 进度条（用 FrameLayout 模拟原生条）
            val barH = dp(10)
            val barContainer = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, barH, 1f).apply { marginStart = dp(4); marginEnd = dp(4) }
                // 背景
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE; cornerRadius = barH / 2f
                    setColor(resources.getColor(R.color.bg_tertiary, null))
                }
            }
            // 前景填充
            val fill = View(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(0, barH, Gravity.START or Gravity.CENTER_VERTICAL)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE; cornerRadius = barH / 2f
                    setColor(resources.getColor(R.color.accent, null))
                }
            }
            barContainer.addView(fill)
            fill.post {
                val w = (barContainer.width * value).toInt().coerceAtLeast(0)
                fill.layoutParams = FrameLayout.LayoutParams(w, barH)
                fill.requestLayout()
            }
            traitRow.addView(barContainer)
            // 数值
            traitRow.addView(TextView(requireContext()).apply {
                text = "${(value * 100).toInt()}%"
                setTextColor(resources.getColor(R.color.accent, null)); textSize = 12f; setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(dp(40), WRAP_CONTENT, Gravity.END.toFloat())
            })
            root.addView(traitRow)
        }

        // ========== System Prompt ==========
        root.addView(TextView(requireContext()).apply {
            text = "系统提示词"; setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f; setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16); bottomMargin = dp(8) }
        })
        root.addView(TextView(requireContext()).apply {
            text = p.prompt.ifEmpty { "No system prompt set" }
            setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f; setLineSpacing(0f, 1.6f)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(resources.getColor(R.color.bg_secondary, null)); cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(150))
        })

        // ========== 底部按钮 ==========
        val btnRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(20), 0, 0)
        }
        // 返回
        btnRow.addView(TextView(requireContext()).apply {
            text = "返回"; setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 14f
            gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(10) }
            setOnClickListener { parentFragmentManager.popBackStack() }
        })
        // 开始对话
        btnRow.addView(TextView(requireContext()).apply {
            text = "开始对话"; setTextColor(Color.WHITE); textSize = 14f; setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(resources.getColor(R.color.accent, null)); cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 2f)
            setOnClickListener {
                viewModel.setActivePersona(p.id)
                parentFragmentManager.popBackStack()
                parentFragmentManager.popBackStack()
            }
        })
        root.addView(btnRow)
    }

    /** 弹出设置弹窗 */
    private fun showSettingsDialog(p: Persona) {
        PersonaSettingsDialog.newInstance(p).show(parentFragmentManager, "settings")
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
