package com.gusogst.chat.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
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
import com.gusogst.chat.ui.theme.ThemeController

class MainActivity : AppCompatActivity() {

    private lateinit var themeController: ThemeController
    private lateinit var settingsManager: ChatSettingsManager
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var ambientOverlay: View
    private lateinit var transitionOverlay: View

    // Navigation views
    private lateinit var navChat: LinearLayout
    private lateinit var navPersona: LinearLayout
    private lateinit var navProviders: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var navIndicator: View

    private var currentTabIndex = 0
    private var isAnimating = false

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
        // 初始化主题控制器（在 super.onCreate 之前）
        themeController = ThemeController.getInstance(this)
        settingsManager = ChatSettingsManager(this)
        
        // 应用保存的主题模式
        applySavedTheme()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initNavigation()
        applyThemeToViews()
        
        // 加载初始 Fragment
        if (savedInstanceState == null) {
            switchFragment(0, animate = false)
        }
        
        // 设置环境光背景
        setupAmbientBackground()
    }

    private fun applySavedTheme() {
        val mode = themeController.getThemeMode()
        val nightMode = when (mode) {
            ThemeController.MODE_LIGHT, ThemeController.MODE_PURE_WHITE -> 
                AppCompatDelegate.MODE_NIGHT_NO
            ThemeController.MODE_DARK, ThemeController.MODE_PURE_BLACK -> 
                AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        
        // 应用 Activity 级别的系统栏颜色
        applySystemBarsTheme(mode)
    }

    private fun applySystemBarsTheme(mode: String) {
        val window = window
        
        val (statusBarColor, navBarColor, isLight) = when (mode) {
            ThemeController.MODE_LIGHT, ThemeController.MODE_PURE_WHITE -> 
                Triple(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), true)
            ThemeController.MODE_DARK -> 
                Triple(0xFF0D0D2B.toInt(), 0xFF0D0D2B.toInt(), false)
            ThemeController.MODE_PURE_BLACK -> 
                Triple(android.graphics.Color.BLACK, android.graphics.Color.BLACK, false)
            else -> {
                val isNight = (resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                if (isNight) {
                    Triple(0xFF0D0D2B.toInt(), 0xFF0D0D2B.toInt(), false)
                } else {
                    Triple(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), true)
                }
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = statusBarColor
            window.navigationBarColor = navBarColor
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (isLight) {
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    private fun initViews() {
        fragmentContainer = findViewById(R.id.fragmentContainer)
        navChat = findViewById(R.id.navChat)
        navPersona = findViewById(R.id.navPersona)
        navProviders = findViewById(R.id.navProviders)
        navSettings = findViewById(R.id.navSettings)
        navIndicator = findViewById(R.id.navIndicator)
        
        // 创建过渡动画覆盖层
        transitionOverlay = View(this).apply {
            setBackgroundColor(getThemeBackgroundColor())
            alpha = 0f
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        (findViewById<View>(android.R.id.content) as? ViewGroup)?.addView(transitionOverlay)
    }

    private fun getThemeBackgroundColor(): Int {
        val mode = themeController.getThemeMode()
        return when (mode) {
            ThemeController.MODE_LIGHT, ThemeController.MODE_PURE_WHITE -> 
                0xFFFFFFFF.toInt()
            ThemeController.MODE_DARK -> 
                0xFF0D0D2B.toInt()
            ThemeController.MODE_PURE_BLACK -> 
                android.graphics.Color.BLACK
            else -> 0xFF0D0D2B.toInt()
        }
    }

    private fun initNavigation() {
        navItems.forEachIndexed { index, item ->
            item.setOnClickListener { 
                if (!isAnimating && currentTabIndex != index) {
                    switchFragment(index)
                }
            }
            
            // 应用按钮按压效果
            applyPressEffect(item)
        }
    }

    private fun applyPressEffect(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
                android.view.MotionEvent.ACTION_UP, 
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            }
            false
        }
    }

    private fun applyThemeToViews() {
        // 应用主题到各个 UI 元素
        val isDark = themeController.isDarkTheme()
        val primaryColor = if (isDark) 0xFF0D0D2B.toInt() else 0xFFFFFFFF.toInt()
        val textColor = themeController.getTextColor()
        val secondaryTextColor = themeController.getSecondaryTextColor()
        val accentColor = themeController.getAccentColorInt()
        
        // 应用到导航栏
        navItems.forEachIndexed { index, item ->
            val icon = item.getChildAt(0) as? ImageView
            val text = item.getChildAt(1) as? TextView
            
            val isActive = index == currentTabIndex
            icon?.setColorFilter(if (isActive) accentColor else secondaryTextColor)
            text?.setTextColor(if (isActive) accentColor else secondaryTextColor)
        }
        
        // 应用到 Header
        findViewById<TextView>(R.id.tvHeaderTitle)?.apply {
            setTextColor(textColor)
        }
    }

    private fun switchFragment(index: Int, animate: Boolean = true) {
        if (isAnimating) return
        isAnimating = true
        
        val previousIndex = currentTabIndex
        currentTabIndex = index
        
        // 更新导航状态
        updateNavigationState()
        
        // 切换 Fragment
        val fragment = fragments[index]
        val transaction = supportFragmentManager.beginTransaction()
        
        if (animate) {
            // Fragment 进入动画
            transaction.setCustomAnimations(
                R.anim.page_enter,
                R.anim.page_exit
            )
        }
        
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
        
        // 更新 Header 标题
        updateHeaderTitle(index)
        
        // 动画结束后重置状态
        if (animate) {
            fragmentContainer.postDelayed({
                isAnimating = false
            }, 400)
        } else {
            isAnimating = false
        }
    }

    private fun updateNavigationState() {
        val isDark = themeController.isDarkTheme()
        val accentColor = themeController.getAccentColorInt()
        val inactiveColor = themeController.getSecondaryTextColor()
        
        navItems.forEachIndexed { i, item ->
            val isActive = i == currentTabIndex
            val icon = item.getChildAt(0) as? ImageView
            val text = item.getChildAt(1) as? TextView
            
            // 颜色过渡动画
            icon?.animate()
                ?.colorFilter(if (isActive) accentColor else inactiveColor)
                ?.setDuration(200)
                ?.start()
            
            text?.animate()
                ?.textColor(if (isActive) accentColor else inactiveColor)
                ?.setDuration(200)
                ?.start()
        }
        
        // 移动指示器
        moveIndicatorWithAnimation(currentTabIndex)
    }

    private fun moveIndicatorWithAnimation(tabIndex: Int) {
        val activeTab = navItems[tabIndex]
        activeTab.post {
            val targetX = activeTab.left + (activeTab.width - navIndicator.width) / 2f
            
            // 使用属性动画实现平滑移动
            val animator = ValueAnimator.ofFloat(navIndicator.x, targetX)
            animator.duration = 350
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                navIndicator.x = animation.animatedValue as Float
            }
            animator.start()
            
            // 缩放动画
            navIndicator.animate()
                .scaleX(1.2f)
                .setDuration(175)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    navIndicator.animate()
                        .scaleX(1f)
                        .setDuration(175)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
                .start()
        }
    }

    private fun updateHeaderTitle(index: Int) {
        val title = when (index) {
            0 -> getString(R.string.nav_chat)
            1 -> getString(R.string.nav_persona)
            2 -> getString(R.string.nav_providers)
            3 -> getString(R.string.nav_settings)
            else -> getString(R.string.app_name)
        }
        
        findViewById<TextView>(R.id.tvHeaderTitle)?.apply {
            animate()
                ?.alpha(0f)
                ?.setDuration(150)
                ?.withEndAction {
                    text = title
                    animate()
                        ?.alpha(1f)
                        ?.setDuration(150)
                        ?.start()
                }
                ?.start()
        }
    }

    private fun setupAmbientBackground() {
        // 环境光背景已经在布局中通过 XML 设置
        // 这里可以添加动态调整或动画效果
    }

    /**
     * 应用主题切换（带动画）- 纯本地函数，不依赖 HTTP
     */
    fun applyThemeWithAnimation(newMode: String) {
        if (isAnimating) return
        isAnimating = true
        
        // 1. 淡出当前界面
        animateFadeOut {
            // 2. 应用新主题
            themeController.setThemeMode(newMode)
            applySavedTheme()
            
            // 3. 更新所有 UI 元素
            applyThemeToViews()
            applySystemBarsTheme(newMode)
            
            // 4. 淡入新界面
            animateFadeIn {
                isAnimating = false
                
                // 5. 重新创建 Activity 以应用所有更改
                recreate()
            }
        }
    }

    /**
     * 应用纯白主题
     */
    fun applyPureWhiteTheme() {
        applyThemeWithAnimation(ThemeController.MODE_PURE_WHITE)
    }

    /**
     * 应用纯黑主题 (AMOLED)
     */
    fun applyPureBlackTheme() {
        applyThemeWithAnimation(ThemeController.MODE_PURE_BLACK)
    }

    /**
     * 应用深色主题
     */
    fun applyDarkTheme() {
        applyThemeWithAnimation(ThemeController.MODE_DARK)
    }

    /**
     * 应用浅色主题
     */
    fun applyLightTheme() {
        applyThemeWithAnimation(ThemeController.MODE_LIGHT)
    }

    /**
     * 应用跟随系统主题
     */
    fun applySystemTheme() {
        applyThemeWithAnimation(ThemeController.MODE_SYSTEM)
    }

    private fun animateFadeOut(onEnd: () -> Unit) {
        val content = findViewById<View>(android.R.id.content) as? ViewGroup ?: return
        
        // 背景过渡
        transitionOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { onEnd() }
            .start()
        
        // 内容淡出
        content.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun animateFadeIn(onEnd: () -> Unit) {
        val content = findViewById<View>(android.R.id.content) as? ViewGroup ?: return
        
        // 背景过渡
        transitionOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        // 内容淡入
        content.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { onEnd() }
            .start()
    }

    /**
     * 快速切换主题（无动画）
     */
    fun quickSwitchTheme(mode: String) {
        themeController.setThemeMode(mode)
        applySavedTheme()
        applyThemeToViews()
        applySystemBarsTheme(mode)
    }

    /**
     * 获取当前主题模式
     */
    fun getCurrentThemeMode(): String {
        return themeController.getThemeMode()
    }

    /**
     * 获取主题控制器
     */
    fun getThemeController(): ThemeController {
        return themeController
    }

    override fun onResume() {
        super.onResume()
        // 确保主题状态正确
        applyThemeToViews()
    }
}
