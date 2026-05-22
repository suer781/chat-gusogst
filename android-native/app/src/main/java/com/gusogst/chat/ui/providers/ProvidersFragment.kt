package com.gusogst.chat.ui.providers

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.UIProvider
import com.gusogst.chat.viewmodel.ChatViewModel

class ProvidersFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var container: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_providers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.providersContainer)
        viewModel.providers.observe(viewLifecycleOwner) { buildList(it) }
    }

    private fun buildList(providers: List<UIProvider>) {
        container.removeAllViews()
        val title = TextView(requireContext()).apply {
            text = "服务商"
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }
        container.addView(title)
        for (provider in providers) addProviderRow(provider)
    }

    private fun addProviderRow(provider: UIProvider) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val info = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(requireContext()).apply {
            text = provider.name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f
        })
        info.addView(TextView(requireContext()).apply {
            text = provider.models.size.toString() + " models"
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 12f
        })
        val sw = Switch(requireContext()).apply {
            isChecked = provider.enabled
            thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.accent, null))
        }
        sw.setOnCheckedChangeListener { _, checked ->
            provider.enabled = checked
            viewModel.saveProviders(viewModel.providers.value.orEmpty())
        }
        row.addView(info)
        row.addView(sw)
        container.addView(row)
        container.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { marginStart = dp(16) }
            setBackgroundColor(resources.getColor(R.color.divider, null))
        })
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
