package com.gusogst.chat.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
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

    private val totalPages = 4
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        settingsManager = ChatSettingsManager(this)

        // 检查是否是首次启动
        if (settingsManager.getLaunchCount() > 0) {
            // 已完成向导，直接进入主页面
            launchMainActivity()
            return
        }

        initViews()
        setupViewPager()
        setupIndicators()
        playFadeInAnimation()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.onboarding_view_pager)
        indicatorContainer = findViewById(R.id.onboarding_indicator_container)
        nextButton = findViewById(R.id.onboarding_next)
        skipText = findViewById(R.id.onboarding_skip)
        prevText = findViewById(R.id.onboarding_prev)

        // Apply initial animations
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onboarding_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

    private fun setupViewPager() {
        viewPager.adapter = OnboardingPagerAdapter(this)
        viewPager.isUserInputEnabled = true

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                updateIndicators()
                updateNavButtons()
            }
        })
    }

    private fun setupIndicators() {
        for (i in 0 until totalPages) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                if (i == 0) 16.dpToPx() else 8.dpToPx(),
                8.dpToPx()
            )
            params.setMargins(4.dpToPx(), 0, 4.dpToPx(), 0)
            dot.layoutParams = params
            dot.setBackgroundResource(
                if (i == 0) R.drawable.indicator_dot_active
                else R.drawable.indicator_dot
            )
            dot.alpha = if (i == 0) 1f else 0.3f
            indicatorContainer.addView(dot)
        }
    }

    private fun updateIndicators() {
        for (i in 0 until indicatorContainer.childCount) {
            val dot = indicatorContainer.getChildAt(i)
            val params = dot.layoutParams as LinearLayout.LayoutParams

            val isActive = i == currentPage

            val scaleAnim = ObjectAnimator.ofFloat(dot, "scaleX", if (isActive) 2f else 1f)
            val alphaAnim = ObjectAnimator.ofFloat(dot, "alpha", if (isActive) 1f else 0.3f)

            val animSet = AnimatorSet()
            animSet.playTogether(scaleAnim, alphaAnim)
            animSet.duration = 300
            animSet.interpolator = DecelerateInterpolator()
            animSet.start()

            if (isActive) {
                params.width = 16.dpToPx()
                dot.setBackgroundResource(R.drawable.indicator_dot_active)
            } else {
                params.width = 8.dpToPx()
                dot.setBackgroundResource(R.drawable.indicator_dot)
            }

            dot.layoutParams = params
        }
    }

    private fun updateNavButtons() {
        when (currentPage) {
            0 -> {
                skipText.visibility = View.VISIBLE
                prevText.visibility = View.GONE
                nextButton.text = getString(R.string.onboarding_next)
            }
            totalPages - 1 -> {
                skipText.visibility = View.GONE
                prevText.visibility = View.VISIBLE
                nextButton.text = getString(R.string.onboarding_start_button)
                nextButton.setTextColor(Color.BLACK)
            }
            else -> {
                skipText.visibility = View.VISIBLE
                prevText.visibility = View.VISIBLE
                nextButton.text = getString(R.string.onboarding_next)
                nextButton.setTextColor(Color.BLACK)
            }
        }
    }

    private fun playFadeInAnimation() {
        val rootView = findViewById<View>(R.id.onboarding_root)
        rootView.alpha = 0f
        rootView.scaleX = 0.95f
        rootView.scaleY = 0.95f

        ObjectAnimator.ofFloat(rootView, View.ALPHA, 0f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(rootView, View.SCALE_X, 0.95f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(rootView, View.SCALE_Y, 0.95f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun completeOnboarding() {
        settingsManager.setLaunchCount(1)

        if (currentPage == totalPages - 1) {
            // Window zoom animation
            val rootView = findViewById<View>(R.id.onboarding_root)
            val readyRoot = findViewById<View>(R.id.ready_root) ?: rootView

            val scaleX = ObjectAnimator.ofFloat(readyRoot, View.SCALE_X, 1f, 30f)
            val scaleY = ObjectAnimator.ofFloat(readyRoot, View.SCALE_Y, 1f, 30f)
            val alpha = ObjectAnimator.ofFloat(readyRoot, View.ALPHA, 1f, 0f)

            scaleX.duration = 600
            scaleY.duration = 600
            alpha.duration = 500
            alpha.startDelay = 100

            scaleX.interpolator = AccelerateDecelerateInterpolator()
            scaleY.interpolator = AccelerateDecelerateInterpolator()
            alpha.interpolator = AccelerateDecelerateInterpolator()

            val set = AnimatorSet()
            set.playTogether(scaleX, scaleY, alpha)
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    launchMainActivity()
                }
            })
            set.start()
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

    private inner class OnboardingPagerAdapter(activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            val greeting1 = view.findViewById<TextView>(R.id.greeting_text_1)
            val greeting2 = view.findViewById<TextView>(R.id.greeting_text_2)
            val desc = view.findViewById<TextView>(R.id.welcome_desc)

            greeting1?.animate()?.alpha(1f)?.setDuration(400)?.start()
            Handler(Looper.getMainLooper()).postDelayed({
                greeting2?.animate()?.alpha(1f)?.setDuration(400)?.start()
                Handler(Looper.getMainLooper()).postDelayed({
                    desc?.animate()?.alpha(1f)?.setDuration(400)?.start()
                }, 200)
            }, 200)
        }, 300)
    }
}

class OnboardingPersonaFragment : Fragment(R.layout.fragment_onboarding_persona)
class OnboardingModelFragment : Fragment(R.layout.fragment_onboarding_model)

class OnboardingReadyFragment : Fragment(R.layout.fragment_onboarding_ready)
