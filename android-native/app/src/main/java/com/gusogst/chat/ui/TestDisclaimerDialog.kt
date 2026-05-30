package com.gusogst.chat.ui

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.gusogst.chat.R

class TestDisclaimerDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dp = resources.displayMetrics.density
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding((24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt())
            setBackgroundColor(resources.getColor(R.color.bg_primary, null))
        }
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.test_disclaimer_title); textSize = 18f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(0, 0, 0, (12*dp).toInt())
        })
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.test_disclaimer_msg)
            textSize = 14f; setTextColor(resources.getColor(R.color.text_tertiary, null)); gravity = Gravity.CENTER
            setPadding(0, 0, 0, (16*dp).toInt())
        })
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.test_disclaimer_ok); textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding((12*dp).toInt(), (10*dp).toInt(), (12*dp).toInt(), (10*dp).toInt())
            setBackgroundColor(resources.getColor(R.color.accent, null)); setOnClickListener { dismiss() }
        })
        return AlertDialog.Builder(requireContext()).setView(root).create().apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
}
