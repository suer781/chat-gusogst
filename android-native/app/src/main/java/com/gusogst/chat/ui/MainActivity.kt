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
import com.gusogst.chat.util.HdrHelper
import com.gusogst.chat.util.MaterialAnimator
import com.gusogst.chat.util.HapticsHelper

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var haptics: HapticsHelper

    private lateinit var tvHeaderTitle: TextView

    /**
     * Represents a single bottom-navigation tab.
     *
     * @param container  The tab's root LinearLayout (clickable area).
     * @param icon       The icon ImageView (color-animated on select).
     * @param text       The label TextView (color-animated on select).
     * @param fragment   The Fragment to show when this tab is active.
     * @param title      Header title string when this tab is active.
     */
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

        // 环境光背景 — 比 Web CSS radial-gradient 更高效（单层 GPU drawable）
        MaterialAnimator.setAmbientBackground(
            findViewById(android.R.id.content),
            R.drawable.bg_ambient
        )

        viewModel.settings.observe(this) { s ->
            applyTheme(s.theme)
            applyGlassEffect(findViewById(R.id.header), s.glassEnabled)
            applyHdrEffect(s.hdrEnabled, s.theme)
        }

        // 首次启动的淡入动画
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

    // ---------------------------------------------------------------
    //  Window insets (status bar, navigation bar, notch, punch-hole)
    // ---------------------------------------------------------------

    /**
     * Apply system-bar insets as padding so content avoids the
     * status bar, notch, cutout, and gesture-navigation area.
     *
     * Two separate listeners:
     *   1. The root content view → top/left/right padding only.
     *   2. The bottom navigation bar → bottom padding only
     *      (the nav bar sits above the gesture pill / nav hint).
     */
    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // -- Root content: avoid status bar / notch on top and sides --
        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            WindowInsetsCompat.CONSUMED
        }

        // -- Bottom nav: add bottom padding for gesture-navigation area --
        val nav = bottomNav
        val navPadV = resources.getDimensionPixelSize(R.dimen.nav_padding_v)
        ViewCompat.setOnApplyWindowInsetsListener(nav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, navPadV, 0, navPadV + systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    // ---------------------------------------------------------------
    //  Theme handling
    // ---------------------------------------------------------------

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "light", "pureWhite" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark", "pureBlack" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
            // 主题切换交叉渐变 — 与 Web 0.6s ease 一致
            MaterialAnimator.applyThemeTransition(findViewById(android.R.id.content))
        }
    }

    /**
     * 玻璃效果 — v2 使用更丰富的渐变层
     * 比 Web 的 CSS blur(20px) + noise texture 更高效：
     * - 使用 GPU 合成渐变层（无 per-element backdrop-filter 重绘）
     * - 无噪声纹理（CSS fractalNoise 在 Web 上高开销）
     */
    private fun applyGlassEffect(view: View?, enabled: Boolean) {
        if (view == null) return
        if (enabled) {
            // 模拟 Web [data-glass="on"] header/nav 的渐变层
            val bg = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    0x23FFFFFF,  // rgba(255,255,255,0.14)
                    0x14B4BEDC,  // rgba(180,190,220,0.08)
                    0x6B0D0D2B   // rgba(13,13,43,0.42)
                )
            )
            bg.cornerRadius = 0f
            view.background = bg
        } else {
            view.setBackgroundResource(R.drawable.bg_header)
        }
    }

    /**
     * Apply HDR glow to header, nav bar, and indicator based on toggle + theme.
     * Uses HdrHelper which mirrors the Web hdr_v3.css glow effect.
     */
    private fun applyHdrEffect(enabled: Boolean, theme: String) {
        val isDark = theme in listOf("dark", "pureBlack", "system")
        val header = findViewById<View>(R.id.header)
        HdrHelper.applyHeaderGlow(header, enabled, isDark)
        HdrHelper.applyNavGlow(bottomNav, enabled, isDark)
        HdrHelper.applyIndicatorGlow(navIndicator, enabled, isDark)
    }

    // ---------------------------------------------------------------
    //  Bottom navigation initialisation
    // ---------------------------------------------------------------

    /**
     * Wire up the 4 bottom-nav tabs and the active-tab indicator.
     *
     * LAYOUT NOTE — navIndicator is an OVERLAY child of the outer
     * FrameLayout (activity_main.xml), NOT a child of bottomNav's
     * horizontal LinearLayout.  See the XML comment block for why.
     *
     * We initialise its position after the first layout pass so
     * the nav items have measurable widths.  We also register a
     * layout-change listener on bottomNav so the indicator
     * re-positions itself whenever insets or orientation change.
     */
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

        // Position indicator on first tab after the first layout pass.
        navIndicator.post { moveIndicator(0, false) }

        // Re-position indicator whenever bottomNav's layout changes
        // (e.g. insets applied, orientation change, padding recalculation).
        bottomNav.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (navItems.isNotEmpty()) {
                val idx = navItems.indexOf(currentNavItem).coerceAtLeast(0)
                moveIndicator(idx, false)
            }
        }
    }

    // ---------------------------------------------------------------
    //  Tab selection & indicator animation
    // ---------------------------------------------------------------

    /**
     * Switch to the given tab: swap fragment, animate icon/text colours,
     * update the header title, and slide the navIndicator to the correct
     * horizontal position.
     */
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

    /**
     * Programmatic "navigate to chat" — used by fragments or external
     * callers to jump back to the first tab.
     */
    fun navigateToChat() {
        if (navItems.isNotEmpty()) selectNav(navItems[0])
    }

    // ---------------------------------------------------------------
    //  Active-tab indicator positioning
    //
    //  The navIndicator View is an OVERLAY in the outer FrameLayout,
    //  positioned using absolute X coordinates.  This is the most
    //  robust approach because:
    //
    //    a) The indicator doesn't take space inside the horizontal
    //       LinearLayout — no layout-weight interference.
    //    b) We use View.setX() which sets the absolute visual position
    //       within the parent (FrameLayout), independent of any
    //       system-bar padding or linear-layout child ordering.
    //    c) View.setX() is not cumulative — calling it repeatedly
    //       always sets the same absolute target, unlike translationX
    //       which is relative to the view's last laid-out position.
    //
    //  COORDINATE-SYSTEM NOTE:
    //    navItem is nested inside bottomNav → inner LinearLayout →
    //    FrameLayout.  navIndicator is a DIRECT child of FrameLayout.
    //    navItem.getX() returns the X within bottomNav, NOT within
    //    the FrameLayout.  FrameLayout may have padding (from
    //    system-bar insets), so the coordinate spaces don't match.
    //
    //    To bridge the gap we use getLocationOnScreen() on both the
    //    target (navItem) and the reference (FrameLayout), then
    //    compute the relative offset within the FrameLayout.
    //    This works regardless of nesting depth or padding.
    //
    //  OLD APPROACH #1 (removed):
    //    The indicator was the 5th child of bottomNav's horizontal
    //    LinearLayout.  Code used translationX to slide it left
    //    across the 4 tabs.  This broke when system-bar insets were
    //    applied, because translationX is relative to the view's
    //    ORIGINAL layout position, not the post-inset position.
    //
    //  OLD APPROACH #2 (removed):
    //    Moved indicator to FrameLayout overlay but computed
    //    targetX from navItem.x (relative to bottomNav), ignoring
    //    the FrameLayout's padding offset.  Result: leftward offset.
    // ---------------------------------------------------------------

    /**
     * Move the navIndicator to the horizontal centre of the tab at
     * [index].  Uses screen coordinates to bridge view hierarchy
     * nesting — the indicator lives in the FrameLayout, while the
     * tabs live in a deeply nested LinearLayout.
     *
     * @param index   The 0-based tab index.
     * @param animate Whether to animate the transition.
     */
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