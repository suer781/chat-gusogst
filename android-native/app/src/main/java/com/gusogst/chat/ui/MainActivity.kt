package com.gusogst.chat.ui

import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatDelegate
import android.view.View
import android.view.WindowManager
import android.view.HapticFeedbackConstants
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.activity.viewModels
import com.gusogst.chat.R
import com.gusogst.chat.ui.chat.ChatFragment
import com.gusogst.chat.model.UISettings
import com.gusogst.chat.viewmodel.ChatViewModel
import com.gusogst.chat.ui.persona.PersonaFragment
import com.gusogst.chat.ui.providers.ProvidersFragment
import com.gusogst.chat.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()

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
    private lateinit var navIndicator: View
    private lateinit var bottomNav: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        setupWindowInsets()
        initNav()
        if (savedInstanceState == null) selectNav(navItems[0])

        viewModel.settings.observe(this) { s ->
            applyTheme(s.theme)
            applyGlassEffect(findViewById(R.id.header), s.glassEnabled)
        }
    }

    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Edge-to-edge: allow content in display cutout area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        // Apply insets to individual views instead of root to avoid
        // squeezing bottomNav width (which breaks moveIndicator calculation)
        val header = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(header) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            WindowInsetsCompat.CONSUMED
        }
        val nav = findViewById<View>(R.id.bottomNav)
        ViewCompat.setOnApplyWindowInsetsListener(nav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "light", "pureWhite" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark", "pureBlack" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun applyGlassEffect(view: View?, enabled: Boolean) {
        if (view == null) return
        if (enabled) {
            view.setBackgroundColor(0x331A1A2E)
        } else {
            view.setBackgroundResource(R.drawable.bg_header)
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
            item.container.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                selectNav(item)
            }
            navItems.add(item)
        }
        bottomNav = findViewById(R.id.bottomNav)
        navIndicator = findViewById(R.id.navIndicator)
        // Position indicator on first tab after layout
        navIndicator.post { moveIndicator(0, false) }
    }

    private fun selectNav(item: NavItem) {
        if (item == currentNavItem) return
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, item.fragment)
            .commit()
        val activeColor = ContextCompat.getColor(this, R.color.nav_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)
        for (nav in navItems) {
            val targetColor = if (nav == item) activeColor else inactiveColor
            // Animate icon color
            val evaluator = android.animation.ArgbEvaluator()
            val iconAnim = android.animation.ValueAnimator.ofObject(evaluator, inactiveColor, targetColor)
            iconAnim.duration = 200
            iconAnim.addUpdateListener { nav.icon.setColorFilter(it.animatedValue as Int) }
            iconAnim.start()
            // Animate text color
            val textAnim = android.animation.ValueAnimator.ofObject(evaluator, nav.text.currentTextColor, targetColor)
            textAnim.duration = 200
            textAnim.addUpdateListener { nav.text.setTextColor(it.animatedValue as Int) }
            textAnim.start()
        }
        tvHeaderTitle.text = item.title
        val index = navItems.indexOf(item)
        moveIndicator(index, true)
        currentNavItem = item
    }

    fun navigateToChat() {
        if (navItems.isNotEmpty()) selectNav(navItems[0])
    }

    private fun moveIndicator(index: Int, animate: Boolean) {
        val navCount = navItems.size
        val navWidth = bottomNav.width
        if (navWidth == 0) return
        val tabWidth = navWidth / navCount
        val indicatorWidth = navIndicator.width
        val targetX = tabWidth * index + (tabWidth - indicatorWidth) / 2f
        if (animate) {
            navIndicator.animate()
                .translationX(targetX)
                .setDuration(200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else {
            navIndicator.translationX = targetX
        }
    }
}
