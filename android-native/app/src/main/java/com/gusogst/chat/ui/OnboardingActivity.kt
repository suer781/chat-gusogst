package com.gusogst.chat.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.gusogst.chat.R
import com.gusogst.chat.data.settings.ChatSettingsManager

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var settingsManager: ChatSettingsManager
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var skipText: TextView
    private lateinit var prevText: TextView
    private lateinit var navContainer: View
    private lateinit var vibrator: Vibrator

    private val totalPages = 4
    private var currentPage = 0
    
    // 独特的自定义插值器
    private val customEase by lazy { PathInterpolator(0.34f, 1.56f, 0.64f, 1f) }
    private val gentleEase by lazy { PathInterpolator(0.2f, 0.8f, 0.2f, 1f) }
    private val bounceEase by lazy { PathInterpolator(0.68f, -0.55f, 0.265f, 1.55f) }
    private val softOvershoot by lazy { OvershootInterpolator(0.8f) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        settingsManager = ChatSettingsManager(this)
        
        // 初始化线性马达震动器
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // 检查是否是首次启动
        if (settingsManager.getLaunchCount() > 0) {
            launchMainActivity()
            return
        }

        initViews()
        setupViewPager()
        setupIndicators()
        playEntranceAnimation()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.onboarding_view_pager)
        indicatorContainer = findViewById(R.id.onboarding_indicator_container)
        nextButton = findViewById(R.id.onboarding_next)
        skipText = findViewById(R.id.onboarding_skip)
        prevText = findViewById(R.id.onboarding_prev)
        navContainer = findViewById(R.id.onboarding_nav_container)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onboarding_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 按钮微交互
        setupButtonMicroAnimations()

        nextButton.setOnClickListener {
            if (currentPage == totalPages - 1) {
                completeOnboarding()
            } else {
                viewPager.currentItem = currentPage + 1
            }
        }

        skipText.setOnClickListener {
            completeOnboarding()
        }

        prevText.setOnClickListener {
            if (currentPage > 0) {
                viewPager.currentItem = currentPage - 1
            }
        }
    }

    private fun setupButtonMicroAnimations() {
        listOf(nextButton, skipText, prevText).forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        // 触觉反馈：轻微触感
                        performLightHaptic()
                        
                        // 按下时：轻微缩小 + 微妙的颜色加深
                        v.animate()
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .translationZ(-2f)
                            .setDuration(100)
                            .setInterpolator(gentleEase)
                            .start()
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        // 释放时：带有呼吸感的回弹动画
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationZ(0f)
                            .setDuration(350)
                            .setInterpolator(softOvershoot)
                            .start()
                    }
                }
                false
            }
        }
        
        // 为按钮添加持续的呼吸效果
        startBreathingAnimation(nextButton)
    }
    
    private fun startBreathingAnimation(view: View?) {
        view ?: return
        
        val animator = ValueAnimator.ofFloat(1f, 1.02f, 1f)
        animator.duration = 2500
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = gentleEase
        animator.addUpdateListener { anim ->
            val value = anim.animatedValue as Float
            view.scaleX = value
            view.scaleY = value
        }
        animator.start()
    }

    private fun setupViewPager() {
        viewPager.adapter = OnboardingPagerAdapter(this)
        viewPager.isUserInputEnabled = true
        viewPager.setPageTransformer { page, position ->
            applyPageTransformations(page, position)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                
                // 页面切换触觉反馈
                performMediumHaptic()
                
                updateIndicators()
                updateNavButtons()
            }
        })
    }

    private fun applyPageTransformations(page: View, position: Float) {
        when {
            position < -1 -> { // 左侧屏幕外
                page.alpha = 0f
                page.scaleX = 0.8f
                page.scaleY = 0.8f
            }
            position <= 1 -> { // 屏幕内
                val absPos = Math.abs(position)
                
                // 渐变透明度
                page.alpha = 1f - absPos * 0.4f
                
                // 独特的缩放和位移效果 - 从中心点向外扩散
                val scale = if (position < 0) {
                    // 左侧页面：向内收缩并向左滑动
                    0.85f + (1f - absPos) * 0.15f
                } else {
                    // 右侧页面：从右滑入，先放大再恢复
                    val t = 1f - absPos
                    0.8f + t * 0.2f
                }
                
                page.scaleX = scale
                page.scaleY = scale
                
                // 独特的位移曲线
                val translation = if (position < 0) {
                    position * page.width * 0.6f
                } else {
                    position * page.width * 0.8f
                }
                page.translationX = translation
                
                // 轻微的垂直位移，创造层次感
                page.translationY = absPos * 20f
            }
            else -> { // 右侧屏幕外
                page.alpha = 0f
                page.scaleX = 0.8f
                page.scaleY = 0.8f
            }
        }
    }

    private fun setupIndicators() {
        for (i in 0 until totalPages) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                8.dpToPx(),
                8.dpToPx()
            )
            params.setMargins(6.dpToPx(), 0, 6.dpToPx(), 0)
            dot.layoutParams = params
            dot.setBackgroundResource(
                if (i == 0) R.drawable.indicator_dot_active
                else R.drawable.indicator_dot
            )
            dot.alpha = if (i == 0) 1f else 0.4f
            dot.tag = if (i == 0) "active" else "inactive"
            indicatorContainer.addView(dot)
        }
    }

    private fun updateIndicators() {
        for (i in 0 until indicatorContainer.childCount) {
            val dot = indicatorContainer.getChildAt(i)
            val params = dot.layoutParams as LinearLayout.LayoutParams

            val isActive = i == currentPage
            val wasActive = dot.tag == "active"
            
            if (isActive != wasActive) {
                dot.tag = if (isActive) "active" else "inactive"
                
                val animSet = AnimatorSet()
                val anims = mutableListOf<Animator>()
                
                if (isActive) {
                    // 激活状态动画：脉冲式展开
                    val scaleX = ObjectAnimator.ofFloat(dot, View.SCALE_X, 1f, 1.4f, 1f)
                    val scaleY = ObjectAnimator.ofFloat(dot, View.SCALE_Y, 1f, 1.4f, 1f)
                    val alpha = ObjectAnimator.ofFloat(dot, View.ALPHA, dot.alpha, 1f)
                    val widthAnim = ValueAnimator.ofInt(params.width, 6.dpToPx(), 20.dpToPx()).apply {
                        addUpdateListener { anim ->
                            params.width = anim.animatedValue as Int
                            dot.requestLayout()
                        }
                    }
                    
                    dot.setBackgroundResource(R.drawable.indicator_dot_active)
                    anims.addAll(listOf(scaleX, scaleY, alpha, widthAnim))
                } else {
                    // 非激活状态动画：优雅收缩
                    val scaleX = ObjectAnimator.ofFloat(dot, View.SCALE_X, dot.scaleX, 0.8f, 1f)
                    val scaleY = ObjectAnimator.ofFloat(dot, View.SCALE_Y, dot.scaleY, 0.8f, 1f)
                    val alpha = ObjectAnimator.ofFloat(dot, View.ALPHA, dot.alpha, 0.35f)
                    val widthAnim = ValueAnimator.ofInt(params.width, 6.dpToPx()).apply {
                        addUpdateListener { anim ->
                            params.width = anim.animatedValue as Int
                            dot.requestLayout()
                        }
                    }
                    
                    dot.setBackgroundResource(R.drawable.indicator_dot)
                    anims.addAll(listOf(scaleX, scaleY, alpha, widthAnim))
                }
                
                animSet.playTogether(anims)
                animSet.duration = 500
                animSet.interpolator = bounceEase
                animSet.start()
            }
        }
    }

    private fun updateNavButtons() {
        val animDuration = 250L
        
        when (currentPage) {
            0 -> {
                animateViewVisibility(skipText, View.VISIBLE, animDuration)
                animateViewVisibility(prevText, View.GONE, animDuration)
                nextButton.text = getString(R.string.onboarding_next)
                nextButton.setTextColor(Color.BLACK)
            }
            totalPages - 1 -> {
                animateViewVisibility(skipText, View.GONE, animDuration)
                animateViewVisibility(prevText, View.VISIBLE, animDuration)
                nextButton.text = getString(R.string.onboarding_start_button)
                nextButton.setTextColor(Color.BLACK)
            }
            else -> {
                animateViewVisibility(skipText, View.VISIBLE, animDuration)
                animateViewVisibility(prevText, View.VISIBLE, animDuration)
                nextButton.text = getString(R.string.onboarding_next)
                nextButton.setTextColor(Color.BLACK)
            }
        }
    }

    private fun animateViewVisibility(view: View, visibility: Int, duration: Long) {
        if (view.visibility == visibility) return
        
        if (visibility == View.VISIBLE) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setInterpolator(smoothEaseOut)
                .start()
        } else {
            view.animate()
                .alpha(0f)
                .translationY(-10f)
                .setDuration(duration)
                .setInterpolator(fastOutSlowIn)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = visibility
                        view.translationY = 0f
                    }
                })
                .start()
        }
    }

    private fun playEntranceAnimation() {
        val rootView = findViewById<View>(R.id.onboarding_root)
        
        // 重置状态
        rootView.alpha = 0f
        viewPager.alpha = 0f
        viewPager.scaleX = 0.85f
        viewPager.scaleY = 0.85f
        navContainer.alpha = 0f
        navContainer.translationY = 80f
        
        // 整个页面：先模糊淡入，再清晰
        ObjectAnimator.ofFloat(rootView, View.ALPHA, 0f, 1f).apply {
            duration = 800
            interpolator = gentleEase
            start()
        }
        
        // ViewPager 入场：从中心放大并旋转轻微角度
        Handler(Looper.getMainLooper()).postDelayed({
            viewPager.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(900)
                .setInterpolator(customEase)
                .start()
        }, 200)
        
        // 导航区域入场：弹性向上弹出
        Handler(Looper.getMainLooper()).postDelayed({
            navContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(bounceEase)
                .start()
            
            // 按钮逐个弹出，带有不同的延迟和曲线
            val buttons = listOf(prevText, indicatorContainer, nextButton, skipText)
            buttons.forEachIndexed { index, view ->
                view.scaleX = 0.7f
                view.scaleY = 0.7f
                view.alpha = 0f
                view.translationY = 30f
                
                Handler(Looper.getMainLooper()).postDelayed({
                    val animSet = AnimatorSet()
                    animSet.playTogether(
                        ObjectAnimator.ofFloat(view, View.SCALE_X, 0.7f, 1f),
                        ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.7f, 1f),
                        ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
                        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 30f, 0f)
                    )
                    animSet.duration = 450
                    animSet.interpolator = softOvershoot
                    animSet.start()
                }, 150 + index * 80L)
            }
        }, 450)
    }

    private fun completeOnboarding() {
        settingsManager.setLaunchCount(1)
        
        // 成功触觉反馈序列
        performSuccessHaptic()

        if (currentPage == totalPages - 1) {
            // 强力触觉反馈
            Handler(Looper.getMainLooper()).postDelayed({
                performHeavyHaptic()
            }, 200)
            
            // 独特的漩涡式消失效果
            val rootView = findViewById<View>(R.id.onboarding_root)
            val readyRoot = findViewById<View>(R.id.ready_root) ?: rootView
            
            // 阶段1：内容轻微旋转并闪烁
            val scaleX1 = ObjectAnimator.ofFloat(readyRoot, View.SCALE_X, 1f, 1.05f)
            val scaleY1 = ObjectAnimator.ofFloat(readyRoot, View.SCALE_Y, 1f, 1.05f)
            val rotation1 = ObjectAnimator.ofFloat(readyRoot, View.ROTATION, 0f, 2f)
            val set1 = AnimatorSet()
            set1.playTogether(scaleX1, scaleY1, rotation1)
            set1.duration = 200
            set1.interpolator = gentleEase
            
            // 阶段2：漩涡式收缩并淡出
            val scaleX2 = ObjectAnimator.ofFloat(rootView, View.SCALE_X, 1f, 0f)
            val scaleY2 = ObjectAnimator.ofFloat(rootView, View.SCALE_Y, 1f, 0f)
            val rotation2 = ObjectAnimator.ofFloat(rootView, View.ROTATION, 2f, -8f)
            val alpha = ObjectAnimator.ofFloat(rootView, View.ALPHA, 1f, 0f)
            val set2 = AnimatorSet()
            set2.playTogether(scaleX2, scaleY2, rotation2, alpha)
            set2.duration = 550
            set2.interpolator = customEase
            
            // 顺序播放
            val fullSet = AnimatorSet()
            fullSet.play(set2).after(set1)
            fullSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    launchMainActivity()
                }
            })
            fullSet.start()
        } else {
            launchMainActivity()
        }
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
    
    // 触觉反馈辅助方法
    private fun performLightHaptic() {
        // 轻微的触感反馈 - 用于按钮按下
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, 50))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }
    
    private fun performMediumHaptic() {
        // 中等触感反馈 - 用于页面切换
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 100))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }
    
    private fun performHeavyHaptic() {
        // 强力触感反馈 - 用于完成动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, 180))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }
    
    private fun performSuccessHaptic() {
        // 成功触觉反馈序列 - 用于完成向导
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val timings = longArrayOf(0, 30, 50, 40, 80, 60)
            val amplitudes = intArrayOf(0, 80, 0, 120, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 50, 40, 80, 60)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 30, 50, 40, 80, 60), -1)
        }
    }

    private inner class OnboardingPagerAdapter(activity: FragmentActivity) :
        androidx.viewpager2.adapter.FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = totalPages

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OnboardingWelcomeFragment()
                1 -> OnboardingPersonaFragment()
                2 -> OnboardingModelFragment()
                3 -> OnboardingReadyFragment()
                else -> OnboardingWelcomeFragment()
            }
        }
    }
}

class OnboardingWelcomeFragment : Fragment(R.layout.fragment_onboarding_welcome) {
    
    private val customEase by lazy { PathInterpolator(0.34f, 1.56f, 0.64f, 1f) }
    private val gentleEase by lazy { PathInterpolator(0.2f, 0.8f, 0.2f, 1f) }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val greeting1 = view.findViewById<TextView>(R.id.greeting_text_1)
        val greeting2 = view.findViewById<TextView>(R.id.greeting_text_2)
        val desc = view.findViewById<TextView>(R.id.welcome_desc)
        
        // 初始状态
        listOf(greeting1, greeting2, desc).forEach { textView ->
            textView?.alpha = 0f
            textView?.scaleX = 0.6f
            textView?.scaleY = 0.6f
            textView?.translationY = 60f
        }
        
        // 错落有致的入场动画，带有缩放和弹性
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            animateInWithScale(greeting1, 0L)
            animateInWithScale(greeting2, 180L)
            animateInWithScale(desc, 360L)
        }, 350)
        
        // 多元素的浮动和呼吸效果
        startWaveAnimation(greeting1, 0f)
        startWaveAnimation(greeting2, 0.33f)
        startBreathingEffect(desc)
    }
    
    private fun animateInWithScale(view: View?, delay: Long) {
        view ?: return
        
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            val animSet = AnimatorSet()
            animSet.playTogether(
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_X, 0.6f, 1.1f, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.6f, 1.1f, 1f),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 60f, 0f)
            )
            animSet.duration = 700
            animSet.interpolator = customEase
            animSet.start()
        }, delay)
    }
    
    private fun startWaveAnimation(view: View?, phase: Float) {
        view ?: return
        
        val animator = ValueAnimator.ofFloat(0f, 1f, 0f)
        animator.duration = 4000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = gentleEase
        animator.addUpdateListener { anim ->
            val value = anim.animatedValue as Float
            val adjustedValue = ((value + phase) % 1f)
            view.translationY = Math.sin(adjustedValue * Math.PI * 2).toFloat() * 8f
        }
        animator.start()
    }
    
    private fun startBreathingEffect(view: View?) {
        view ?: return
        
        val animator = ValueAnimator.ofFloat(1f, 1.03f, 1f)
        animator.duration = 3500
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = gentleEase
        animator.addUpdateListener { anim ->
            val value = anim.animatedValue as Float
            view.scaleX = value
            view.scaleY = value
        }
        animator.start()
    }
}

class OnboardingPersonaFragment : Fragment(R.layout.fragment_onboarding_persona) {
    
    private val customEase by lazy { PathInterpolator(0.34f, 1.56f, 0.64f, 1f) }
    private val softOvershoot by lazy { OvershootInterpolator(0.8f) }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val title = view.findViewById<TextView>(R.id.persona_title)
        val desc = view.findViewById<TextView>(R.id.persona_desc)
        val cardsContainer = view.findViewById<ViewGroup>(R.id.persona_cards_container)
        
        // 初始状态
        title?.alpha = 0f
        title?.scaleX = 0.8f
        title?.scaleY = 0.8f
        title?.translationX = -40f
        desc?.alpha = 0f
        desc?.translationY = 25f
        cardsContainer?.alpha = 0f
        cardsContainer?.scaleY = 0.9f
        
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            // 标题动画：从左侧滑入并放大
            val titleAnim = AnimatorSet()
            titleAnim.playTogether(
                ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(title, View.SCALE_X, 0.8f, 1f),
                ObjectAnimator.ofFloat(title, View.SCALE_Y, 0.8f, 1f),
                ObjectAnimator.ofFloat(title, View.TRANSLATION_X, -40f, 0f)
            )
            titleAnim.duration = 500
            titleAnim.interpolator = customEase
            titleAnim.start()
            
            Handler(android.os.Looper.getMainLooper()).postDelayed({
                // 描述动画
                desc?.animate()
                    ?.alpha(1f)
                    ?.translationY(0f)
                    ?.setDuration(450)
                    ?.setInterpolator(customEase)
                    ?.start()
                
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    // 卡片容器动画
                    val containerAnim = AnimatorSet()
                    containerAnim.playTogether(
                        ObjectAnimator.ofFloat(cardsContainer, View.ALPHA, 0f, 1f),
                        ObjectAnimator.ofFloat(cardsContainer, View.SCALE_Y, 0.9f, 1f)
                    )
                    containerAnim.duration = 400
                    containerAnim.interpolator = customEase
                    containerAnim.start()
                    
                    // 卡片逐个动画：以交错的方式弹出
                    for (i in 0 until cardsContainer.childCount) {
                        val card = cardsContainer.getChildAt(i)
                        card.scaleX = 0.6f
                        card.scaleY = 0.6f
                        card.alpha = 0f
                        card.rotation = if (i % 2 == 0) -8f else 8f
                        
                        Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val cardAnim = AnimatorSet()
                            cardAnim.playTogether(
                                ObjectAnimator.ofFloat(card, View.SCALE_X, 0.6f, 1.08f, 1f),
                                ObjectAnimator.ofFloat(card, View.SCALE_Y, 0.6f, 1.08f, 1f),
                                ObjectAnimator.ofFloat(card, View.ALPHA, 0f, 1f),
                                ObjectAnimator.ofFloat(card, View.ROTATION, card.rotation, 0f)
                            )
                            cardAnim.duration = 500
                            cardAnim.interpolator = softOvershoot
                            cardAnim.start()
                        }, i * 120L)
                    }
                }, 180)
            }, 120)
        }, 250)
    }
}

class OnboardingModelFragment : Fragment(R.layout.fragment_onboarding_model) {
    
    private val customEase by lazy { PathInterpolator(0.34f, 1.56f, 0.64f, 1f) }
    private val gentleEase by lazy { PathInterpolator(0.2f, 0.8f, 0.2f, 1f) }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val title = view.findViewById<TextView>(R.id.model_title)
        val desc = view.findViewById<TextView>(R.id.model_desc)
        val itemsContainer = view.findViewById<ViewGroup>(R.id.model_items_container)
        
        // 初始状态
        title?.alpha = 0f
        title?.scaleX = 0.9f
        title?.scaleY = 0.9f
        title?.translationY = -30f
        desc?.alpha = 0f
        desc?.translationY = 20f
        itemsContainer?.alpha = 0f
        
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            // 标题动画：从上方优雅落下
            val titleAnim = AnimatorSet()
            titleAnim.playTogether(
                ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(title, View.SCALE_X, 0.9f, 1.05f, 1f),
                ObjectAnimator.ofFloat(title, View.SCALE_Y, 0.9f, 1.05f, 1f),
                ObjectAnimator.ofFloat(title, View.TRANSLATION_Y, -30f, 0f)
            )
            titleAnim.duration = 550
            titleAnim.interpolator = customEase
            titleAnim.start()
            
            Handler(android.os.Looper.getMainLooper()).postDelayed({
                // 描述动画
                desc?.animate()
                    ?.alpha(1f)
                    ?.translationY(0f)
                    ?.setDuration(400)
                    ?.setInterpolator(gentleEase)
                    ?.start()
                
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    // 列表项动画：从右侧瀑布式滑入
                    itemsContainer?.alpha = 1f
                    for (i in 0 until itemsContainer.childCount) {
                        val item = itemsContainer.getChildAt(i)
                        item.translationX = 150f
                        item.alpha = 0f
                        item.scaleX = 0.85f
                        item.scaleY = 0.85f
                        
                        Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val itemAnim = AnimatorSet()
                            itemAnim.playTogether(
                                ObjectAnimator.ofFloat(item, View.TRANSLATION_X, 150f, -10f, 0f),
                                ObjectAnimator.ofFloat(item, View.ALPHA, 0f, 1f),
                                ObjectAnimator.ofFloat(item, View.SCALE_X, 0.85f, 1.02f, 1f),
                                ObjectAnimator.ofFloat(item, View.SCALE_Y, 0.85f, 1.02f, 1f)
                            )
                            itemAnim.duration = 500
                            itemAnim.interpolator = customEase
                            itemAnim.start()
                        }, i * 90L)
                    }
                }, 150)
            }, 100)
        }, 280)
    }
}

class OnboardingReadyFragment : Fragment(R.layout.fragment_onboarding_ready) {
    
    private val customEase by lazy { PathInterpolator(0.34f, 1.56f, 0.64f, 1f) }
    private val gentleEase by lazy { PathInterpolator(0.2f, 0.8f, 0.2f, 1f) }
    private val bounceEase by lazy { PathInterpolator(0.68f, -0.55f, 0.265f, 1.55f) }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val title = view.findViewById<TextView>(R.id.ready_title)
        val desc = view.findViewById<TextView>(R.id.ready_desc)
        
        // 初始状态
        title?.scaleX = 0.4f
        title?.scaleY = 0.4f
        title?.alpha = 0f
        title?.rotation = -15f
        desc?.alpha = 0f
        desc?.translationY = 40f
        desc?.scaleX = 0.9f
        desc?.scaleY = 0.9f
        
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            // 标题动画：中心爆炸式弹出
            val titleAnim = AnimatorSet()
            titleAnim.playTogether(
                ObjectAnimator.ofFloat(title, View.SCALE_X, 0.4f, 1.15f, 1f),
                ObjectAnimator.ofFloat(title, View.SCALE_Y, 0.4f, 1.15f, 1f),
                ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(title, View.ROTATION, -15f, 0f)
            )
            titleAnim.duration = 800
            titleAnim.interpolator = bounceEase
            titleAnim.start()
            
            Handler(android.os.Looper.getMainLooper()).postDelayed({
                // 描述动画：优雅淡入
                val descAnim = AnimatorSet()
                descAnim.playTogether(
                    ObjectAnimator.ofFloat(desc, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(desc, View.TRANSLATION_Y, 40f, 0f),
                    ObjectAnimator.ofFloat(desc, View.SCALE_X, 0.9f, 1f),
                    ObjectAnimator.ofFloat(desc, View.SCALE_Y, 0.9f, 1f)
                )
                descAnim.duration = 550
                descAnim.interpolator = customEase
                descAnim.start()
            }, 250)
        }, 280)
        
        // 为标题添加持续的庆祝式呼吸效果
        startCelebrationPulse(title)
    }
    
    private fun startCelebrationPulse(view: View?) {
        view ?: return
        
        val animator = ValueAnimator.ofFloat(1f, 1.04f, 1f)
        animator.duration = 2000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = gentleEase
        animator.addUpdateListener { anim ->
            val value = anim.animatedValue as Float
            view.scaleX = value
            view.scaleY = value
        }
        animator.start()
    }
}
