package com.gusogst.chat.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
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
import com.gusogst.chat.util.HdrHelper

class MainActivity : AppCompatActivity() {

    private lateinit var themeController: ThemeController
    private lateinit var settingsManager: ChatSettingsManager
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var transitionOverlay: View

    // Navigation views
    private lateinit var navChat: LinearLayout
    private lateinit var navPersona: LinearLayout
    private lateinit var navProviders: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var navIndicator: View

    private var currentTabIndex = 0
    private var isAnimating = false

    // Animation interpolators - 精心调优的丝滑插值器
    private val smoothEaseOut by lazy { PathInterpolator(0.25f, 0.1f, 0.25f, 1f) } // iOS风格
    private val smoothEaseInOut by lazy { PathInterpolator(0.42f, 0f, 0.58f, 1f) } // Material Design
    private val overshoot by lazy { OvershootInterpolator(1.2f) } // 轻微回弹
    private val quickEaseOut by lazy { DecelerateInterpolator(1.5f) } // 快速减速

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
        
        // 检查是否是首次启动
        checkFirstLaunch()
        
        // 应用保存的主题模式（必须在 super.onCreate 之前）
        applyThemeBeforeCreate()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 请求最高刷新率（关键！）
        requestHighestRefreshRate()

        initViews()
        initNavigation()
        applyThemeToViews()
        
        // 预加载主题资源（性能优化）
        window.decorView.post {
            HdrHelper.preloadResources(window.decorView)
        }
        
        // 加载初始 Fragment
        if (savedInstanceState == null) {
            switchFragment(0, animate = false)
        }
        
        // 设置环境光背景
        setupAmbientBackground()
    }
    
    /**
     * 检查是否是首次启动
     * 0: 新用户，首次打开
     * 1: 已完成向导的用户
     */
    private fun checkFirstLaunch() {
        val launchCount = settingsManager.getLaunchCount()

        if (launchCount == 0) {
            // 首次启动 - 显示向导
            android.util.Log.i("FirstLaunch", "首次启动应用，启动向导页面")
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 非首次启动 - 直接进入应用
            android.util.Log.i("FirstLaunch", "应用已启动过 $launchCount 次，直接进入")
        }
    }

    /**
     * 请求手机能支持的最高刷新率（120/144/240FPS）
     */
    private fun requestHighestRefreshRate() {
        try {
            // 获取所有支持的刷新率
            val display = windowManager.defaultDisplay
            val supportedModes = display.supportedModes
            
            // 找到刷新率最高的模式
            val bestMode = supportedModes.maxByOrNull { it.refreshRate }
            
            if (bestMode != null) {
                // 应用最佳模式
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = bestMode.modeId
                }
                
                // 打印日志（可选）
                android.util.Log.i("RefreshRate", "Requesting ${bestMode.refreshRate}Hz refresh rate")
            }
            
            // 请求最高帧率（Android 11+）
            window.setPreferMinimalPostProcessing(false)
            
            // 禁用 FPS 限制
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            // 降级处理：如果上面的方法不行，尝试简单方法
            try {
                window.attributes = window.attributes.apply {
                    refreshRate = Float.MAX_VALUE
                }
            } catch (e2: Exception) {
                // 忽略错误
            }
        }
    }

    private fun applyThemeBeforeCreate() {
        val mode = themeController.getThemeMode()
        when (mode) {
            ThemeController.MODE_LIGHT, ThemeController.MODE_PURE_WHITE -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.Theme_ChatGusogst)
            }
            ThemeController.MODE_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.Theme_ChatGusogst)
            }
            ThemeController.MODE_PURE_BLACK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.Theme_ChatGusogst_Amoled)
            }
            else -> {
                // 跟随系统
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                setTheme(R.style.Theme_ChatGusogst)
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
                0xFF000000.toInt()
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
                    // 使用 Hardware Layer 优化动画
                    v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    v.animate()
                        .scaleX(0.94f)
                        .scaleY(0.94f)
                        .setDuration(60)
                        .setInterpolator(quickEaseOut)
                        .withEndAction { v.setLayerType(View.LAYER_TYPE_NONE, null) }
                        .start()
                }
                android.view.MotionEvent.ACTION_UP, 
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .setInterpolator(overshoot)
                        .withEndAction { v.setLayerType(View.LAYER_TYPE_NONE, null) }
                        .start()
                }
            }
            false
        }
    }

    private fun applyThemeToViews() {
        // 应用主题到各个 UI 元素
        val isDark = themeController.isDarkTheme()
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
            
            // 颜色过渡动画 - 使用 Hardware Layer
            icon?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            icon?.animate()
                ?.colorFilter(if (isActive) accentColor else inactiveColor)
                ?.setDuration(150)
                ?.setInterpolator(smoothEaseOut)
                ?.withEndAction { icon?.setLayerType(View.LAYER_TYPE_NONE, null) }
                ?.start()
            
            text?.animate()
                ?.textColor(if (isActive) accentColor else inactiveColor)
                ?.setDuration(150)
                ?.setInterpolator(smoothEaseOut)
                ?.start()
        }
        
        // 移动指示器 - 使用 Hardware Layer
        moveIndicatorWithAnimation(currentTabIndex)
    }

    private fun moveIndicatorWithAnimation(tabIndex: Int) {
        val activeTab = navItems[tabIndex]
        activeTab.post {
            val targetX = activeTab.left + (activeTab.width - navIndicator.width) / 2f
            
            // 使用 Hardware Layer 优化动画性能
            navIndicator.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 使用属性动画实现平滑移动
            val animator = ValueAnimator.ofFloat(navIndicator.x, targetX)
            animator.duration = 200
            animator.interpolator = smoothEaseOut
            animator.addUpdateListener { animation ->
                navIndicator.x = animation.animatedValue as Float
            }
            animator.start()
            
            // 缩放动画
            navIndicator.animate()
                .scaleX(1.15f)
                .setDuration(100)
                .setInterpolator(smoothEaseInOut)
                .withEndAction {
                    navIndicator.animate()
                        .scaleX(1f)
                        .setDuration(100)
                        .setInterpolator(smoothEaseInOut)
                        .withEndAction { 
                            navIndicator.setLayerType(View.LAYER_TYPE_NONE, null)
                        }
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
                ?.setDuration(100)
                ?.setInterpolator(smoothEaseOut)
                ?.withEndAction {
                    text = title
                    animate()
                        ?.alpha(1f)
                        ?.setDuration(100)
                        ?.setInterpolator(smoothEaseOut)
                        ?.start()
                }
                ?.start()
        }
    }

    private fun setupAmbientBackground() {
        // 环境光背景已经在布局中通过 XML 设置
    }

    /**
     * 应用主题切换（带动画）- 纯本地函数，不依赖 HTTP
     */
    fun applyThemeWithAnimation(newMode: String) {
        if (isAnimating) return
        isAnimating = true
        
        // 1. 清理旧的 Drawable 缓存
        HdrHelper.clearCache()
        
        // 2. 预加载新主题资源（后台线程）
        window.decorView.post {
            HdrHelper.preloadResources(window.decorView)
        }
        
        // 3. 淡出当前界面（使用 Hardware Layer）
        animateFadeOut {
            // 4. 应用新主题并重新创建 Activity
            themeController.setThemeMode(newMode)
            recreate()
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
        
        // 设置正确的背景颜色
        transitionOverlay.setBackgroundColor(getThemeBackgroundColor())
        
        // 使用 Hardware Layer 优化动画
        transitionOverlay.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        // 背景过渡
        transitionOverlay.animate()
            .alpha(1f)
            .setDuration(200)
            .setInterpolator(smoothEaseOut)
            .withEndAction {
                transitionOverlay.setLayerType(View.LAYER_TYPE_NONE, null)
                onEnd()
            }
            .start()
        
        // 内容淡出
        content.animate()
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(smoothEaseOut)
            .start()
    }

    /**
     * 快速切换主题（无动画，直接应用）
     */
    fun quickSwitchTheme(mode: String) {
        themeController.setThemeMode(mode)
        applySavedTheme()
        applyThemeToViews()
    }

    /**
     * 获取主题控制器
     */
    fun getThemeController(): ThemeController {
        return themeController
    }
}
