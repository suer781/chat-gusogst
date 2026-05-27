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
import android.view.animation.DecelerateInterpolator
import android.animation.ValueAnimator
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
import com.gusogst.chat.util.HdrHelper
import com.gusogst.chat.util.MaterialAnimator
import com.gusogst.chat.util.HapticsHelper

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var haptics: HapticsHelper

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
        haptics = HapticsHelper(this)
        initNav()
        setupWindowInsets()
        if (savedInstanceState == null) selectNav(navItems[0])

        MaterialAnimator.setAmbientBackground(findViewById(android.R.id.content))

        viewModel.settings.observe(this) { s ->
            applyTheme(s.theme)
            applyGlassAndHdr(
                findViewById(R.id.header),
                s.glassEnabled, s.hdrEnabled,
                s.theme
            )
        }

        if (savedInstanceState == null) {
            val contentRoot = findViewById<View>(android.R.id.content)
            contentRoot.alpha = 0f
            contentRoot.post {
                contentRoot.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            WindowInsetsCompat.CONSUMED
        }
        val nav = bottomNav
        val navPadV = resources.getDimensionPixelSize(R.dimen.nav_padding_v)
        ViewCompat.setOnApplyWindowInsetsListener(nav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, navPadV, 0, navPadV + systemBars.bottom)
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
            MaterialAnimator.applyThemeTransition(findViewById(android.R.id.content))
        }
    }

    /**
     * 综合玻璃 + HDR 效果
     * glass 控制透光模糊感，HDR 控制辉光反光层
     */
    private fun applyGlassAndHdr(view: View?, glassEnabled: Boolean, hdrEnabled: Boolean, theme: String) {
        if (view == null) return
        val isDark = theme in listOf("dark", "pureBlack", "system")
        HdrHelper.applyGlassWithHdr(view, hdrEnabled, glassEnabled, isDark)
        HdrHelper.applyNavGlow(bottomNav, hdrEnabled, isDark)
        HdrHelper.applyIndicatorGlow(navIndicator, hdrEnabled, isDark)
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
                haptics.glassTap()
                selectNav(item)
            }
            navItems.add(item)
        }
        bottomNav = findViewById(R.id.bottomNav)
        navIndicator = findViewById(R.id.navIndicator)
        navIndicator.post { moveIndicator(0, false) }
        bottomNav.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (navItems.isNotEmpty()) {
                val idx = navItems.indexOf(currentNavItem).coerceAtLeast(0)
                moveIndicator(idx, false)
            }
        }
    }

    private fun selectNav(item: NavItem) {
        if (item == currentNavItem) return
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, item.fragment)
            .commit()
        val activeColor = ContextCompat.getColor(this, R.color.nav_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)
        for (nav in navItems) {
            val targetColor = if (nav == item) activeColor else inactiveColor
            val evaluator = android.animation.ArgbEvaluator()
            val iconAnim = android.animation.ValueAnimator.ofObject(evaluator, inactiveColor, targetColor)
            iconAnim.duration = 200
            iconAnim.addUpdateListener { nav.icon.setColorFilter(it.animatedValue as Int) }
            iconAnim.start()
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
        if (navItems.isEmpty()) return
        val navItem = navItems[index].container
        if (navItem.width == 0) {
            navItem.viewTreeObserver.addOnGlobalLayoutListener(
                object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        navItem.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        moveIndicator(index, animate)
                    }
                }
            )
            return
        }
        if (navIndicator.width == 0) return
        val navItemLoc = IntArray(2)
        navItem.getLocationOnScreen(navItemLoc)
        val navCenterX = navItemLoc[0] + navItem.width / 2f
        val parentLoc = IntArray(2)
        (navIndicator.parent as View).getLocationOnScreen(parentLoc)
        val targetX = navCenterX - parentLoc[0] - navIndicator.width / 2f
        if (animate) {
            MaterialAnimator.animateIndicator(navIndicator, targetX, navIndicator.width.toFloat())
        } else {
            navIndicator.x = targetX
        }
    }
}