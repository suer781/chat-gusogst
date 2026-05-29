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
    private var currentTheme: String = "system"
    private var settingsFirstFire = true

    /** 从 SharedPreferences 读取当前保存的主题名 */
    private fun readCurrentTheme(): String = ChatApplication.cachedTheme

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
        // 使用 Application 缓存的主题（避免重复读 SharedPreferences）
        val themeName = ChatApplication.cachedTheme
        currentTheme = themeName
        // pureBlack 需要额外的 Amoled 主题覆盖
        if (themeName == "pureBlack") {
            setTheme(R.style.Theme_ChatGusogst_Amoled)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        haptics = HapticsHelper(this)
        initNav()
        if (savedInstanceState == null) selectNav(navItems[0])
        setupWindowInsets()

        // 推后非关键效果到首帧之后（环境光、入场动画）
        findViewById<View>(android.R.id.content).post {
            currentTheme = readCurrentTheme()
            MaterialAnimator.setAmbientBackground(findViewById(android.R.id.content), currentTheme)
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
                HdrHelper.applyGlassWithHdr(
                    findViewById(R.id.header),
                    s.hdrEnabled, s.glassEnabled, isDark
                )
                HdrHelper.applyGlassWithHdr(
                    findViewById(android.R.id.content),
                    s.hdrEnabled, s.glassEnabled, isDark
                )
                HdrHelper.applyNavGlow(bottomNav, s.hdrEnabled, isDark)
                HdrHelper.applyIndicatorGlow(navIndicator, s.hdrEnabled, isDark)
                MaterialAnimator.setAmbientBackground(
                    findViewById(android.R.id.content),
                    s.theme,
                    animate = themeChanged
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
            header.animate().translationY(0f).alpha(1f).setDuration(350).setInterpolator(DecelerateInterpolator()).start()
            fragmentContainer.postDelayed({
                fragmentContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).setInterpolator(DecelerateInterpolator()).start()
            }, 50)
            bNav.postDelayed({
                bNav.animate().translationY(0f).alpha(1f).setDuration(350).setInterpolator(DecelerateInterpolator()).start()
            }, 100)
            navInd.postDelayed({
                navInd.animate().alpha(1f).setDuration(200).start()
            }, 250)
        }
    }

    /** 从最近任务/后台切回时的快速入场动画 */
    private fun playResumeAnimation() {
        val root = findViewById<View>(android.R.id.content)
        root.alpha = 0.85f
        root.animate()
            .alpha(1f)
            .setDuration(250)
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
        bottomNav.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (navItems.isNotEmpty()) {
                val idx = navItems.indexOf(currentNavItem).coerceAtLeast(0)
                moveIndicator(idx, false)
            }
        }
    }

    private fun selectNav(item: NavItem) {
        if (item == currentNavItem) return

        // 先退出当前页面（120ms），再替换，再进入新页面（200ms）
        val container = findViewById<View>(R.id.fragmentContainer)

        // 获取当前 fragment 的视图做退出动画
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        val exitView = currentFragment?.view

        if (exitView != null) {
            MaterialAnimator.viewExit(exitView, 120) {
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