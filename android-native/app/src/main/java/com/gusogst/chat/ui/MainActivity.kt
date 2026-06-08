package com.gusogst.chat.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
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
import com.gusogst.chat.BuildConfig
import com.gusogst.chat.R
import com.gusogst.chat.data.settings.ChatSettingsManager
import com.gusogst.chat.ui.chat.ChatFragment
import com.gusogst.chat.ui.persona.PersonaFragment
import com.gusogst.chat.ui.providers.ProvidersFragment
import com.gusogst.chat.ui.settings.SettingsFragment
import com.gusogst.chat.ui.theme.ThemeController
import com.gusogst.chat.util.HdrHelper
import com.gusogst.chat.util.RealHdrHelper
import com.gusogst.chat.util.DynamicColorEngine
import com.gusogst.chat.util.PerformanceMonitor

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
    private lateinit var headerView: View
    private lateinit var ambientOverlay: View
    
    private var currentTabIndex = 0
    private var isAnimating = false
    
    // Animation interpolators - 精心调优的丝滑插值器
    private val smoothEaseOut by lazy { PathInterpolator(0.25f, 0.1f, 0.25f, 1f) } // iOS风格
    private val smoothEaseInOut by lazy { PathInterpolator(0.42f, 0f, 0.58f, 1f) } // Material Design
    private val overshoot by lazy { OvershootInterpolator(1.2f) } // 轻微回弹
    private val quickEaseOut by lazy { DecelerateInterpolator(1.5f) } // 快速减速
    private val elasticOut by lazy { PathInterpolator(0.34f, 1.56f, 0.64f, 1f) } // 弹性曲线
    
    // 3D 翻转插值器
    private val flipInterpolator by lazy { PathInterpolator(0.4f, 0f, 0.2f, 1f) }

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
        
        // 应用HDR设置
        applyHdrSettings()

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
        
        // 启动性能监控（调试模式）
        if (BuildConfig.DEBUG) {
            startPerformanceMonitoring()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 恢复性能监控
        if (BuildConfig.DEBUG) {
            startPerformanceMonitoring()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停性能监控
        if (BuildConfig.DEBUG) {
            stopPerformanceMonitoring()
        }
    }
    
    /**
     * 启动性能监控
     */
    private fun startPerformanceMonitoring() {
        PerformanceMonitor.startMonitoring(
            onFpsUpdate = { fps ->
                // FPS 低于 55 时记录警告
                if (fps < 55f) {
                    android.util.Log.w("Performance", "Low FPS: $fps")
                }
            },
            onJank = { frameTime ->
                // 记录卡顿
                android.util.Log.w("Performance", "Jank detected: ${frameTime}ms")
            }
        )
    }
    
    /**
     * 停止性能监控
     */
    private fun stopPerformanceMonitoring() {
        val stats = PerformanceMonitor.stopMonitoring()
        if (!stats.isHealthy) {
            android.util.Log.w("Performance", "Performance issues detected: ${stats.droppedFrames} dropped frames")
        }
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
        headerView = findViewById(R.id.headerView)
        
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
        
        // 创建环境光叠加层（动态背景效果）
        createAmbientOverlay()
        
        // 设置 Header 视差滚动效果
        setupParallaxHeader()
    }
    
    /**
     * 创建动态环境光背景效果
     * 利用径向渐变创造层次感和深度
     */
    private fun createAmbientOverlay() {
        val rootView = findViewById<View>(android.R.id.content) as? ViewGroup ?: return
        
        ambientOverlay = View(this).apply {
            // 硬件加速层
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 动态径向渐变背景
            post {
                val w = width.toFloat()
                val h = height.toFloat()
                
                // 创建多层次径向渐变
                val gradient = android.graphics.RadialGradient(
                    w / 2f, 0f,  // 中心点在顶部
                    Math.max(w, h) * 0.8f,
                    intArrayOf(
                        0x2020B0F0.toInt(),  // 顶部：淡蓝高光
                        0x15003080.toInt(),  // 中部：深蓝
                        0x00000000            // 底部：透明
                    ),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
                
                background = android.graphics.drawable.GradientDrawable(gradient)
                alpha = 0.3f
            }
            
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // 添加到底层
        rootView.addView(ambientOverlay, 0)
    }
    
    /**
     * 设置视差滚动效果
     * Header 随内容滚动产生层次感
     */
    private fun setupParallaxHeader() {
        fragmentContainer.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY != oldScrollY) {
                // 视差偏移量（header 移动速度是内容的一半）
                val parallaxOffset = scrollY * 0.5f
                headerView.translationY = -parallaxOffset
                
                // 动态模糊效果（根据滚动速度）
                val velocity = kotlin.math.abs(scrollY - oldScrollY)
                if (velocity > 20) {
                    headerView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    headerView.alpha = 1f - (velocity - 20) * 0.01f
                } else {
                    headerView.alpha = 1f
                }
            }
        }
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
                    
                    // 3D 按压效果：轻微缩小 + Z轴位移 + 阴影加深
                    v.animate()
                        .scaleX(0.94f)
                        .scaleY(0.94f)
                        .translationZ(-4f)
                        .elevation(v.elevation + 2f)
                        .setDuration(60)
                        .setInterpolator(quickEaseOut)
                        .withEndAction { v.setLayerType(View.LAYER_TYPE_NONE, null) }
                        .start()
                    
                    // 添加波纹效果（如果支持）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        v.isPressed = true
                    }
                }
                android.view.MotionEvent.ACTION_UP, 
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    
                    // 回弹效果：轻微超调 + Z轴恢复 + 阴影恢复
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationZ(0f)
                        .elevation(v.elevation - 2f)
                        .setDuration(180)
                        .setInterpolator(overshoot)
                        .withEndAction {
                            v.setLayerType(View.LAYER_TYPE_NONE, null)
                            v.isPressed = false
                        }
                        .start()
                    
                    // 触觉反馈
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    }
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
            // 3D 翻转页面切换效果
            val previousFragment = fragments.getOrNull(previousIndex)
            
            // 旧页面：向右翻转并淡出
            previousFragment?.let {
                transaction.setCustomAnimations(
                    R.anim.page_flip_out_right,
                    R.anim.page_flip_in_left
                )
            } ?: run {
                // 标准转场动画
                transaction.setCustomAnimations(
                    R.anim.page_enter,
                    R.anim.page_exit
                )
            }
            
            // 添加缩放和淡入效果
            transaction.setCustomAnimations(
                R.anim.page_scale_in,
                R.anim.page_scale_out,
                R.anim.page_scale_in,
                R.anim.page_scale_out
            )
        }
        
        // 执行切换
        transaction.replace(R.id.fragmentContainer, fragment)
        
        // 添加共享元素过渡（如果有的话）
        if (animate) {
            // 添加滑动方向检测，自动决定滑动方向
            val slideDirection = if (index > previousIndex) {
                // 向右滑动（前进）
                R.anim.page_slide_in_right
            } else {
                // 向左滑动（后退）
                R.anim.page_slide_in_left
            }
            
            // 重新设置动画
            transaction.setCustomAnimations(
                slideDirection,
                R.anim.page_slide_out,
                R.anim.page_slide_in_left,
                R.anim.page_slide_out_right
            )
        }
        
        transaction.commit()
        
        // 更新 Header 标题
        updateHeaderTitle(index)
        
        // 触发页面切换完成回调
        if (animate) {
            fragmentContainer.postDelayed({
                isAnimating = false
                
                // 触觉反馈
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.decorView.performHapticFeedback(
                        android.view.HapticFeedbackConstants.CONFIRM
                    )
                }
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
            
            // 使用 Hardware Layer 优化动画
            navIndicator.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 组合动画：位置移动 + 缩放 + 颜色脉冲
            val animatorSet = AnimatorSet()
            
            // 1. 位置动画
            val posAnimator = ValueAnimator.ofFloat(navIndicator.x, targetX)
            posAnimator.duration = 250
            posAnimator.interpolator = elasticOut
            posAnimator.addUpdateListener { animation ->
                navIndicator.x = animation.animatedValue as Float
            }
            
            // 2. 缩放脉冲动画
            val scaleXSmall = ObjectAnimator.ofFloat(navIndicator, View.SCALE_X, 1f, 0.85f, 1.15f, 1f)
            val scaleYSmall = ObjectAnimator.ofFloat(navIndicator, View.SCALE_Y, 1f, 0.85f, 1.15f, 1f)
            scaleXSmall.duration = 300
            scaleYSmall.duration = 300
            scaleXSmall.interpolator = elasticOut
            scaleYSmall.interpolator = elasticOut
            
            // 3. 宽度动画
            val widthAnim = ValueAnimator.ofInt(navIndicator.width, activeTab.width)
            widthAnim.duration = 250
            widthAnim.interpolator = elasticOut
            widthAnim.addUpdateListener { animation ->
                val params = navIndicator.layoutParams
                params.width = animation.animatedValue as Int
                navIndicator.layoutParams = params
            }
            
            // 4. 发光脉冲效果
            val glowAlpha = ObjectAnimator.ofFloat(navIndicator, View.ALPHA, 1f, 0.7f, 1f)
            glowAlpha.duration = 300
            glowAlpha.interpolator = smoothEaseInOut
            
            // 并行播放所有动画
            animatorSet.playTogether(posAnimator, scaleXSmall, scaleYSmall, widthAnim, glowAlpha)
            animatorSet.start()
            
            // 动画结束后清理
            navIndicator.postDelayed({
                navIndicator.setLayerType(View.LAYER_TYPE_NONE, null)
                
                // 触觉反馈
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    navIndicator.performHapticFeedback(
                        android.view.HapticFeedbackConstants.CLOCK_TICK
                    )
                }
            }, 300)
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
    
    /**
     * 应用HDR设置
     */
    private fun applyHdrSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && settingsManager.isHdrEnabled()) {
            try {
                // 自动优化HDR设置
                RealHdrHelper.autoOptimize(this)
                
                // 为整个窗口应用HDR增强（关键）
                window.decorView.let { view ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        view.setRenderEffect(
                            RenderEffect.createColorFilterEffect(
                                createHdrEnhancementColorFilter()
                            )
                        )
                    }
                }
                
                android.util.Log.i("HDR", "HDR模式已启用，毛玻璃增强已激活")
            } catch (e: Exception) {
                android.util.Log.e("HDR", "启用HDR失败: ${e.message}")
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createHdrEnhancementColorFilter(): ColorFilter {
        val matrix = ColorMatrix()
        
        // HDR模式下提升对比度和饱和度
        matrix.setSaturation(1.2f)
        
        // 提高亮度和对比度
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.setScale(1.08f, 1.08f, 1.08f, 1f)
        
        // 对比度增强
        val contrast = 1.05f
        val contrastMatrix = ColorMatrix()
        val scale = contrast
        val trans = -0.5f * scale + 0.5f
        contrastMatrix.set(floatArrayOf(
            scale, 0f, 0f, 0f, trans,
            0f, scale, 0f, 0f, trans,
            0f, 0f, scale, 0f, trans,
            0f, 0f, 0f, 1f, 0f
        ))
        
        matrix.postConcat(brightnessMatrix)
        matrix.postConcat(contrastMatrix)
        
        return ColorMatrixColorFilter(matrix)
    }
    
    /**
     * 刷新HDR设置（从设置页面调用）
     */
    fun refreshHdrSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (settingsManager.isHdrEnabled()) {
                    // 启用HDR
                    RealHdrHelper.enableHdr(this)
                    
                    // 应用HDR增强效果
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        window.decorView.setRenderEffect(
                            RenderEffect.createColorFilterEffect(
                                createHdrEnhancementColorFilter()
                            )
                        )
                    }
                    
                    // 应用动态颜色增强
                    val accentColor = themeController.getAccentColorInt()
                    DynamicColorEngine.animateToColor(
                        themeController.getThemeColorInt(),
                        accentColor
                    ) { _, _ -> }
                    
                    android.util.Log.i("HDR", "HDR已启用，毛玻璃增强已激活")
                } else {
                    // 禁用HDR
                    RealHdrHelper.disableHdr(this)
                    
                    // 清除HDR增强效果
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        window.decorView.setRenderEffect(null)
                    }
                    
                    android.util.Log.i("HDR", "HDR已禁用")
                }
            } catch (e: Exception) {
                android.util.Log.e("HDR", "刷新HDR设置失败: ${e.message}")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理动态颜色引擎
        DynamicColorEngine.cleanup()
        
        // 清理性能监控
        if (BuildConfig.DEBUG) {
            stopPerformanceMonitoring()
        }
    }
}
