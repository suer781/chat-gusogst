package com.gusogst.chat.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gusogst.chat.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val container = view.findViewById<ViewGroup>(R.id.settingsContainer)
            val fragments = listOf(
                BasicSettingsFragment(),
                ModelSettingsFragment(),
                PlatformSettingsFragment(),
                SearchSettingsFragment(),
                MemorySettingsFragment(),
                AboutSettingsFragment()
            )
            for (fragment in fragments) {
                childFragmentManager.beginTransaction()
                    .add(container.id, fragment)
                    .commit()
            }
        }
    }
}
