package com.gusogst.chat.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.gusogst.chat.R
import com.gusogst.chat.ui.chat.ChatFragment
import com.gusogst.chat.ui.persona.PersonaFragment
import com.gusogst.chat.ui.providers.ProvidersFragment
import com.gusogst.chat.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var tvHeaderTitle: TextView

    private data class NavItem(
        val container: LinearLayout,
        val icon: ImageView,
        val text: TextView,
        val fragment: Fragment,
        val title: String
    )

    private val navItems = mutableListOf<NavItem>()
    private var currentNavItem: NavItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        setupWindowInsets()
        initNav()
        if (savedInstanceState == null) selectNav(navItems[0])
    }

    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun initNav() {
        val configs = listOf(
            Triple(R.id.navChat, R.id.navChatIcon, R.id.navChatText) to
                (ChatFragment() to getString(R.string.nav_chat)),
            Triple(R.id.navPersona, R.id.navPersonaIcon, R.id.navPersonaText) to
                (PersonaFragment() to getString(R.string.nav_persona)),
            Triple(R.id.navProviders, R.id.navProvidersIcon, R.id.navProvidersText) to
                (ProvidersFragment() to getString(R.string.nav_providers)),
            Triple(R.id.navSettings, R.id.navSettingsIcon, R.id.navSettingsText) to
                (SettingsFragment() to getString(R.string.nav_settings))
        )
        for ((ids, pair) in configs) {
            val (fragment, title) = pair
            val item = NavItem(
                findViewById(ids.first),
                findViewById(ids.second),
                findViewById(ids.third),
                fragment,
                title
            )
            item.container.setOnClickListener { selectNav(item) }
            navItems.add(item)
        }
    }

    private fun selectNav(item: NavItem) {
        if (item == currentNavItem) return
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, item.fragment)
            .commit()
        val activeColor = ContextCompat.getColor(this, R.color.nav_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)
        for (nav in navItems) {
            val color = if (nav == item) activeColor else inactiveColor
            nav.icon.setColorFilter(color)
            nav.text.setTextColor(color)
        }
        tvHeaderTitle.text = item.title
        currentNavItem = item
    }
}
