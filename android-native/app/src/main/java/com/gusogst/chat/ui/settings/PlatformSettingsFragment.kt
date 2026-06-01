package com.gusogst.chat.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import com.gusogst.chat.R

class PlatformSettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply {
            setBackgroundColor(resources.getColor(R.color.bg_primary, null))
        }
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        // TODO: 由 @osbot.main 智能体填充平台相关设置项
        sv.addView(root)
        return sv
    }
}
