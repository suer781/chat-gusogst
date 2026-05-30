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
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.activity.viewModels
import com.gusogst.chat.R
import com.gusogst.chat.ChatApplication
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
    private var currentTheme: String = ChatApplication.cachedTheme
    private var settingsFirstFire = true
    private var isNavAnimating = false
    private var bottomNavLayoutListener: View.OnLayoutChangeListener? = null

    // 动画时长常量 (ms)
    companion object {
        private const val ENTRY_HEADER_DURATION = 350L
        private const val ENTRY_FRAGMENT_DURATION = 400L
        private const val ENTRY_NAV_DURATION = 350L
        private const val ENTRY_INDICATOR_DURATION = 200L
        private const val ENTRY_FRAGMENT_DELAY = 50L
        private const val ENTRY_NAV_DELAY = 100L
        private const val ENTRY_INDICATOR_DELAY = 250L
        private const val RESUME_FADE_DURATION = 250L
    }

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
        installSplashScreen()
        // 主题设置已移至 ChatApplication.onCreate()，避免双重重建
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        haptics = HapticsHelper(this)
        initNav()
        if (savedInstanceState == null) selectNav(navItems[0])
        setupWindowInsets()

        // 推后非关键效果到首帧之后（环境光、入场动画）
        findViewById<View>(android.R.id.content).post {
            MaterialAnimator.setAmbientBackground(findViewById(android.R.id.content), currentTheme, hdrEnabled = viewModel.settings.value?.hdrEnabled ?: false)
            if (savedInstanceState == null) {
                playEntryAnimation()
            }

            viewModel.settings.observe(this) { s ->
                val themeChanged = s.theme != currentTheme
                currentTheme = s.theme
                if (settingsFirstFire) {
                    settingsFirstFire = false
                } else if (themeChanged) {
                    applyTheme(s.theme)
                }
                val isDark = isDarkTheme(s.theme)
                // Fix 2: properly pass all params; Fix 3: glassEnabled for header combined effect
                HdrHelper.applyHeaderGlow(
                    findViewById(R.id.header),
                    s.hdrEnabled, isDark, s.glassEnabled
                )
                HdrHelper.applyGlassWithHdr(
                    findViewById(android.R.id.content),
                    s.hdrEnabled, s.glassEnabled, isDark
                )
                HdrHelper.applyNavGlow(bottomNav, s.hdrEnabled, isDark)
                // Fix 4: pass glassEnabled for backdrop-filter simulation on indicator
                HdrHelper.applyIndicatorGlow(navIndicator, s.hdrEnabled, isDark, s.glassEnabled)
                // Fix 11: pass hdrEnabled for ambient light saturation
                MaterialAnimator.setAmbientBackground(
                    findViewById(android.R.id.content),
                    s.theme,
                    animate = themeChanged,
                    hdrEnabled = s.hdrEnabled
                )
                applyEyeCare(s.eyeCareMode, s.eyeCareIntensity)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        playResumeAnimation()
    }

    /** 冷启动交错序贯动画（~600ms 总时长） */
    private fun playEntryAnimation() {
        val header = findViewById<View>(R.id.header)
        val fragmentContainer = findViewById<View>(R.id.fragmentContainer)
        val bNav = findViewById<View>(R.id.bottomNav)
        val navInd = findViewById<View>(R.id.navIndicator)

        val offsetY = resources.getDimensionPixelSize(R.dimen.header_height)
        header.translationY = -offsetY.toFloat()
        header.alpha = 0f
        fragmentContainer.alpha = 0f
        fragmentContainer.scaleX = 0.95f
        fragmentContainer.scaleY = 0.95f
        bNav.translationY = resources.getDimensionPixelSize(R.dimen.nav_height).toFloat()
        bNav.alpha = 0f
        navInd.alpha = 0f

        header.post {
            // 并行触发，交错 50ms，600ms 内完成
            header.animate().translationY(0f).alpha(1f).setDuration(ENTRY_HEADER_DURATION).setInterpolator(DecelerateInterpolator()).start()
            fragmentContainer.postDelayed({
                fragmentContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(ENTRY_FRAGMENT_DURATION).setInterpolator(DecelerateInterpolator()).start()
            }, ENTRY_FRAGMENT_DELAY)
            bNav.postDelayed({
                bNav.animate().translationY(0f).alpha(1f).setDuration(ENTRY_NAV_DURATION).setInterpolator(DecelerateInterpolator()).start()
            }, ENTRY_NAV_DELAY)
            navInd.postDelayed({
                navInd.animate().alpha(1f).setDuration(ENTRY_INDICATOR_DURATION).start()
            }, ENTRY_INDICATOR_DELAY)
        }
    }

    /** 从最近任务/后台切回时的快速入场动画 */
    private fun playResumeAnimation() {
        val root = findViewById<View>(android.R.id.content)
        root.alpha = 0.85f
        root.animate()
            .alpha(1f)
            .setDuration(RESUME_FADE_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .start()
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
            // 设置窗口背景色匹配目标主题，避免重建间隙透出白色
            window.setBackgroundDrawableResource(
                when (theme) {
                    "light", "pureWhite" -> android.R.color.white
                    "dark" -> R.color.bg_primary_dark
                    "pureBlack" -> android.R.color.black
                    else -> R.color.bg_primary_dark
                }
            )
            overridePendingTransition(R.anim.theme_enter, R.anim.theme_exit)
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    /** 正确判断当前是否暗色主题（"system" 按实际系统模式） */
    private fun isDarkTheme(theme: String): Boolean = when (theme) {
        "dark", "pureBlack" -> true
        "light", "pureWhite" -> false
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /** 护眼模式暖色滤镜叠加到根布局 */
    private fun applyEyeCare(enabled: Boolean, warmth: Int) {
        val root = findViewById<View>(android.R.id.content)
        if (!enabled || warmth <= 0) {
            root.foreground = null
            return
        }
        // 暖色半透明覆盖层，intensity 控制透明度
        val alpha = (warmth * 0.7f).toInt().coerceIn(0, 80)
        val overlayColor = Color.argb(alpha, 255, 180, 100)
        root.foreground = android.graphics.drawable.ColorDrawable(overlayColor)
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
        bottomNavLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (navItems.isNotEmpty()) {
                val idx = navItems.indexOf(currentNavItem).coerceAtLeast(0)
                moveIndicator(idx, false)
            }
        }
        bottomNav.removeOnLayoutChangeListener(bottomNavLayoutListener)
        bottomNav.addOnLayoutChangeListener(bottomNavLayoutListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        bottomNavLayoutListener?.let { bottomNav.removeOnLayoutChangeListener(it) }
        bottomNavLayoutListener = null
        haptics.destroy()
    }

    private fun selectNav(item: NavItem) {
        if (item == currentNavItem || isNavAnimating) return
        isNavAnimating = true

        val container = findViewById<View>(R.id.fragmentContainer)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        val exitView = currentFragment?.view

        if (exitView != null) {
            exitView.animate().cancel()
            MaterialAnimator.viewExit(exitView, 80) {
                doFragmentReplace(item, container)
            }
        } else {
            doFragmentReplace(item, container)
        }
    }

    private fun doFragmentReplace(item: NavItem, container: View) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.page_enter, 0)
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
        isNavAnimating = false
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