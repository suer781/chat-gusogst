package com.gusogst.chat.ui.settings

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
import com.gusogst.chat.viewmodel.ChatViewModel

class FontSizeFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private val fontSizes = listOf(12, 13, 14, 15, 16, 17, 18, 20, 22)
    private var currentSize = 14

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Header with back button
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(20) }
        }
        header.addView(TextView(requireContext()).apply {
            text = getString(R.string.font_back); setTextColor(resources.getColor(R.color.accent, null)); textSize = 14f
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { parentFragmentManager.popBackStack() }
        })
        header.addView(TextView(requireContext()).apply {
            text = getString(R.string.font_title); setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 18f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(8) }
        })
        root.addView(header)

        // Preview card
        val previewCard =LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(resources.getColor(R.color.bg_secondary, null))
                setStroke(1, resources.getColor(R.color.border_color, null))
            }
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(24) }
            elevation = 2f * resources.displayMetrics.density
        }
        previewCard.addView(TextView(requireContext()).apply {
            text = getString(R.string.font_preview_title); setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 12f
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(8) }
        })
        val previewTv = TextView(requireContext())
        previewCard.addView(previewTv)
        root.addView(previewCard)

        // Slider
        val sliderLabel = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(8) }
        }
        sliderLabel.addView(TextView(requireContext()).apply {
            text = getString(R.string.font_size_label); setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 15f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        val sizeDisplay = TextView(requireContext()).apply {
            textSize = 14f; setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        sliderLabel.addView(sizeDisplay)
        root.addView(sliderLabel)

        val seekBar = SeekBar(requireContext()).apply {
            max = fontSizes.size - 1
            progressTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
            thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(16) }
        }
        root.addView(seekBar)

        // Size option pills
        val pillRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(24) }
        }
        val labels = listOf(getString(R.string.font_size_s), getString(R.string.font_size_m), getString(R.string.font_size_l), getString(R.string.font_size_xl))
        val indices = listOf(0, 2, 5, 8)
        for (i in indices.indices) {
            val idx = indices[i]
            val pill = TextView(requireContext()).apply {
                text = labels[i]; textSize = 12f; gravity = Gravity.CENTER
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setTextColor(resources.getColor(R.color.gray_300, null))
                background = GradientDrawable().apply {
                    cornerRadius = dp(100).toFloat()
                    setColor(resources.getColor(R.color.bg_tertiary, null))
                }
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) }
                setOnClickListener {
                    seekBar.progress = idx
                    updatePreview(previewTv, sizeDisplay, fontSizes[idx])
                    viewModel.updateSettings { it.copy(fontSize = fontSizes[idx].toString()) }
                }
            }
            pillRow.addView(pill)
        }
        root.addView(pillRow)

        // Size matrix - show all sizes
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.font_size_ref_title); setTextColor(resources.getColor(R.color.text_secondary, null)); textSize = 12f
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(8) }
        })
        for (size in fontSizes) {
            val refRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(4), dp(8), dp(4))
                background = if (size == currentSize) GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat(); setColor(resources.getColor(R.color.accent_soft, null))
                } else null
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(2) }
                setOnClickListener {
                    seekBar.progress = fontSizes.indexOf(size)
                    updatePreview(previewTv, sizeDisplay, size)
                    viewModel.updateSettings { it.copy(fontSize = size.toString()) }
                }
            }
            refRow.addView(TextView(requireContext()).apply {
                text = "${size}px"; setTextColor(resources.getColor(R.color.text_tertiary, null)); textSize = 12f
                layoutParams = LinearLayout.LayoutParams(dp(50), WRAP_CONTENT)
            })
            refRow.addView(TextView(requireContext()).apply {
                text = getString(R.string.font_size_ref_text); setTextColor(resources.getColor(R.color.text_primary, null)); textSize = size.toFloat()
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            })
            root.addView(refRow)
        }

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val scroll = view as ScrollView
        val root = scroll.getChildAt(0) as LinearLayout
        val previewTv = (root.getChildAt(1) as LinearLayout).getChildAt(1) as TextView
        val sizeDisplay = (root.getChildAt(2) as LinearLayout).getChildAt(1) as TextView
        val seekBar = root.getChildAt(3) as SeekBar

        viewModel.settings.observe(viewLifecycleOwner) { s ->
            currentSize = try { s.fontSize.toInt() } catch (_: Exception) { 14 }
            val idx = fontSizes.indexOf(currentSize).coerceAtLeast(0)
            seekBar.progress = idx
            updatePreview(previewTv, sizeDisplay, currentSize)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val size = fontSizes[progress]
                updatePreview(previewTv, sizeDisplay, size)
                viewModel.updateSettings { it.copy(fontSize = size.toString()) }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun updatePreview(tv: TextView, label: TextView, size: Int) {
        tv.text = getString(R.string.font_preview_content, size)
        tv.setTextColor(resources.getColor(R.color.text_primary, null))
        tv.textSize = size.toFloat()
        label.text = "${size}px"
        label.setTextColor(resources.getColor(R.color.accent, null))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
