package com.gusogst.chat.ui

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.gusogst.chat.R
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class TestDisclaimerDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dp = resources.displayMetrics.density
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding((24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt())
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_bg))
        }
        root.addView(TextView(requireContext()).apply {
            text = "\u26A0 \u6D4B\u8BD5\u7248\u58F0\u660E"; textSize = 18f; setTextColor(resources.getColor(R.color.white, null)); gravity = Gravity.CENTER
            setPadding(0, 0, 0, (12*dp).toInt())
        })
        root.addView(TextView(requireContext()).apply {
            text = "\u672C\u5E94\u7528\u4E3A\u6D4B\u8BD5\u7248\u672C\uFF0C\u53EF\u80FD\u5B58\u5728 Bug\u3002\u6570\u636E\u4EC5\u4FDD\u5B58\u5728\u672C\u5730\uFF0C\u8BF7\u5B9A\u671F\u5907\u4EFD\u3002"
            textSize = 14f; setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_hint)); gravity = Gravity.CENTER
            setPadding(0, 0, 0, (16*dp).toInt())
        })
        root.addView(TextView(requireContext()).apply {
            text = "\u6211\u77E5\u9053\u4E86"; textSize = 14f; setTextColor(resources.getColor(R.color.white, null)); gravity = Gravity.CENTER
            setPadding((12*dp).toInt(), (10*dp).toInt(), (12*dp).toInt(), (10*dp).toInt())
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent)); setOnClickListener { dismiss() }
        })
        return AlertDialog.Builder(requireContext()).setView(root).create().apply {
            window?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.transparent, null)))
        }
    }
}