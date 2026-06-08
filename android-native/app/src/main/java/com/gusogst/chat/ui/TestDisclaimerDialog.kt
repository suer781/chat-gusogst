package com.gusogst.chat.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class TestDisclaimerDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("测试免责声明")
            .setMessage("此功能仅用于测试目的")
            .setPositiveButton("确定") { _, _ -> dismiss() }
            .create()
    }
}