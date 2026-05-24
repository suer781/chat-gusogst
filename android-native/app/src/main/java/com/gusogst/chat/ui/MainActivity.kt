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

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()

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
    private var currentIndex: Int = -1
    private lateinit var navIndicator: View
    private lateinit var bottomNav: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        initNav()
        setupWindowInsets()
        if (savedInstanceState == null) selectTab(0)

        viewModel.settings.observe(this) { s ->
            applyTheme(s.theme)
            applyGlassEffect(findViewById(R.id.header), s.glassEnabled)
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

    // ---------------------------------------------------------------
    //  Bottom navigation
    //
    //  INTERFACE:  selectTab(index) — single entry point for all
    //              tab switching and indicator positioning.
    //
    //              moveIndicator(index) — private positioning helper.
    //
    //              navigateToChat() — public alias for selectTab(0).
    //
    //  All callers (click handlers, layout-change listener, initial
    //  positioning) route through selectTab().  There is no second
    //  code path — every tab switch applies the same logic.
    // ---------------------------------------------------------------

    /**
     * Wire up the 4 bottom-nav tabs and the active-tab indicator.
     *
     * LAYOUT NOTE — navIndicator is an OVERLAY child of the outer
     * FrameLayout (activity_main.xml), NOT a child of bottomNav's
     * horizontal LinearLayout.  See the XML comment block for why.
     *
     * After wiring, we schedule selectTab(0) on the next layout pass
     * so the nav items have measurable widths.  We also register a
     * layout-change listener on bottomNav so the indicator re-positions
     * itself whenever insets or orientation change.
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
            navItems.add(item)
        }
        bottomNav = findViewById(R.id.bottomNav)
        navIndicator = findViewById(R.id.navIndicator)

        // -- Wire click handlers (all route through selectTab) --
        for ((i, item) in navItems.withIndex()) {
            item.container.setOnClickListener { _ ->
                item.container.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                selectTab(i)
            }
        }

        // -- Initial tab: position after first layout pass --
        navIndicator.post { selectTab(0) }

        // -- Re-position indicator whenever bottomNav's layout changes
        //    (e.g. insets applied, orientation change).
        bottomNav.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (currentIndex >= 0) selectTab(currentIndex)
        }
    }

    /**
     * Central tab-switching interface.
     *
     * Every tab switch — click, programmatic (navigateToChat),
     * layout recovery — goes through this single method.
     *
     * It does three things:
     *   1. Swaps the fragment in the container.
     *   2. Animates icon/text colours across all tabs.
     *   3. Positions the navIndicator under the selected tab.
     *
     * @param index   0-based tab index.
     * @param animate Whether to animate the indicator slide
     *                (default true).  Pass false for layout-change
     *                recovery where animation looks jarring.
     */
    private fun selectTab(index: Int, animate: Boolean = true) {
        if (index !in navItems.indices || index == currentIndex) return
        val item = navItems[index]

        // 1. Swap fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, item.fragment)
            .commit()

        // 2. Animate icon/text colours
        val activeColor = ContextCompat.getColor(this, R.color.nav_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)
        for ((i, nav) in navItems.withIndex()) {
            val targetColor = if (i == index) activeColor else inactiveColor
            val evaluator = android.animation.ArgbEvaluator()
            // Icon
            val iconAnim = ValueAnimator.ofObject(evaluator, inactiveColor, targetColor)
            iconAnim.duration = 200
            iconAnim.addUpdateListener { nav.icon.setColorFilter(it.animatedValue as Int) }
            iconAnim.start()
            // Text
            val textAnim = ValueAnimator.ofObject(evaluator, nav.text.currentTextColor, targetColor)
            textAnim.duration = 200
            textAnim.addUpdateListener { nav.text.setTextColor(it.animatedValue as Int) }
            textAnim.start()
        }

        // 3. Update header
        tvHeaderTitle.text = item.title

        // 4. Position indicator
        moveIndicator(index, animate)

        currentIndex = index
    }

    /**
     * Public-facing shortcut to jump to the chat tab.
     * Used by fragments or external callers.
     */
    fun navigateToChat() {
        selectTab(0)
    }

    // ---------------------------------------------------------------
    //  Active-tab indicator positioning
    //
    //  COORDINATE-SYSTEM NOTE:
    //    navItem is nested inside bottomNav → inner LinearLayout →
    //    FrameLayout.  navIndicator is a DIRECT child of FrameLayout.
    //    getX() would return the X within the direct parent only,
    //    which differs from FrameLayout's coordinate space when the
    //    FrameLayout has system-bar padding.
    //
    //    To eliminate nesting issues we use getLocationOnScreen()
    //    on both the target (navItem) and the reference (FrameLayout),
    //    then compute the relative offset.  This works regardless of
    //    nesting depth or padding.
    //
    //  WHY NOT translationX (v0):
    //    The indicator was the 5th child of bottomNav's horizontal
    //    LinearLayout.  translationX is relative to the view's
    //    ORIGINAL layout position (right of all 4 tabs).  System-bar
    //    padding shifted the layout, breaking the reference → rightward.
    //
    //  WHY NOT getX() directly (v1):
    //    Moved indicator to FrameLayout but used navItem.getX(),
    //    which is relative to bottomNav, not FrameLayout.  FrameLayout
    //    padding caused a coordinate mismatch → leftward.
    // ---------------------------------------------------------------

    /**
     * Private helper: position the navIndicator at the centre of the
     * tab at [index].  Uses screen coordinates to bridge nesting.
     *
     * External callers should use [selectTab] instead.
     */
    private fun moveIndicator(index: Int, animate: Boolean) {
        // Guard: no tabs.
        if (index !in navItems.indices) return

        val navItem = navItems[index].container

        // Guard: tab not yet laid out. Retry on next layout pass.
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

        // Guard: indicator not yet measured.
        if (navIndicator.width == 0) return

        // --- Calculate target X within the FrameLayout ---
        // 1. Nav item centre on screen.
        val navItemLoc = IntArray(2)
        navItem.getLocationOnScreen(navItemLoc)
        val navCenterX = navItemLoc[0] + navItem.width / 2f

        // 2. FrameLayout left edge on screen.
        val parentLoc = IntArray(2)
        (navIndicator.parent as View).getLocationOnScreen(parentLoc)

        // 3. Indicator X = navCenter - FrameLayout left - half indicator.
        val targetX = navCenterX - parentLoc[0] - navIndicator.width / 2f

        if (animate) {
            navIndicator.animate()
                .x(targetX)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            navIndicator.x = targetX
        }
    }
}
