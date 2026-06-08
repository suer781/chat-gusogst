package com.gusogst.chat.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.gusogst.chat.R
import com.gusogst.chat.data.settings.ChatSettingsManager
import com.gusogst.chat.ui.chat.ChatFragment
import com.gusogst.chat.ui.persona.PersonaFragment
import com.gusogst.chat.ui.providers.ProvidersFragment
import com.gusogst.chat.ui.settings.SettingsFragment
import com.gusogst.chat.util.MaterialAnimator

class MainActivity : AppCompatActivity() {

    private lateinit var settingsManager: ChatSettingsManager
    private lateinit var fragmentContainer: View

    // Navigation views
    private lateinit var navChat: LinearLayout
    private lateinit var navPersona: LinearLayout
    private lateinit var navProviders: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var navIndicator: View

    private val navItems by lazy {
        listOf(navChat, navPersona, navProviders, navSettings)
    }

    private val fragments by lazy {
        listOf(
            ChatFragment(),
            PersonaFragment(),
            ProvidersFragment(),
            SettingsFragment()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = ChatSettingsManager(this)
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fragmentContainer = findViewById(R.id.fragmentContainer)

        // Initialize navigation
        initNavigation()

        // Load initial fragment
        if (savedInstanceState == null) {
            switchFragment(0, animate = false)
        }

        // Set ambient background
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        val themeMode = settingsManager.getThemeMode()
        MaterialAnimator.setAmbientBackground(
            rootView as View,
            when (themeMode) {
                "dark", "pureBlack" -> themeMode
                "light", "pureWhite" -> themeMode
                else -> "dark"
            },
            animate = false,
            hdrEnabled = true
        )
    }

    private fun applyTheme() {
        val themeMode = settingsManager.getThemeMode()
        val nightMode = when (themeMode) {
            "dark", "pureBlack" -> AppCompatDelegate.MODE_NIGHT_YES
            "light", "pureWhite" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun initNavigation() {
        navChat = findViewById(R.id.navChat)
        navPersona = findViewById(R.id.navPersona)
        navProviders = findViewById(R.id.navProviders)
        navSettings = findViewById(R.id.navSettings)
        navIndicator = findViewById(R.id.navIndicator)

        navItems.forEachIndexed { index, item ->
            item.setOnClickListener { switchFragment(index) }
            MaterialAnimator.applyButtonEffects(item)
        }
    }

    private fun switchFragment(index: Int, animate: Boolean = true) {
        // Update navigation UI
        navItems.forEachIndexed { i, item ->
            val isActive = i == index
            val icon = item.getChildAt(0) as ImageView
            val text = item.getChildAt(1) as TextView

            val activeColor = getColor(R.color.accent)
            val inactiveColor = getColor(R.color.nav_inactive)

            icon.setColorFilter(if (isActive) activeColor else inactiveColor)
            text.setTextColor(if (isActive) activeColor else inactiveColor)
        }

        // Move indicator to active tab
        moveIndicator(index)

        // Switch fragment
        val fragment = fragments[index]
        val transaction = supportFragmentManager.beginTransaction()

        if (animate) {
            transaction.setCustomAnimations(
                R.anim.page_enter,
                R.anim.page_exit
            )
        }

        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()

        // Update header title
        val headerTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        headerTitle.text = when (index) {
            0 -> getString(R.string.nav_chat)
            1 -> getString(R.string.nav_persona)
            2 -> getString(R.string.nav_providers)
            3 -> getString(R.string.nav_settings)
            else -> getString(R.string.app_name)
        }
    }

    private fun moveIndicator(tabIndex: Int) {
        val activeTab = navItems[tabIndex]
        activeTab.post {
            val targetX = activeTab.left + (activeTab.width - navIndicator.width) / 2f
            MaterialAnimator.animateIndicator(navIndicator, targetX, activeTab.width.toFloat())
        }
    }

    fun applyThemeWithAnimation() {
        val rootView = window.decorView.findViewById<View>(android.R.id.content) as View
        MaterialAnimator.applyThemeTransition(rootView)

        // Recreate activity with animation
        overridePendingTransition(R.anim.theme_enter, R.anim.theme_exit)
        recreate()
    }
}
