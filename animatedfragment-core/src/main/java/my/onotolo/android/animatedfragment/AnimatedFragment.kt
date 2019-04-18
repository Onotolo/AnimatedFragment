package my.onotolo.android.animatedfragment

import android.os.Bundle
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import my.onotolo.android.animatedfragment.listeners.BackButtonEventProvider
import my.onotolo.android.animatedfragment.listeners.BackButtonListener
import my.onotolo.android.animatedfragment.utils.addSimpleOnLayoutChangeListener
import java.util.*

typealias HeightCounter = () -> Float

abstract class AnimatedFragment : Fragment(), BackButtonListener {

    protected val animation = AnimationConfiguration()

    enum class AnimationType {
        SlideFromTop, SlideFromBottom, AlphaAnimation
    }

    class AnimationConfiguration internal constructor() {
        val animatedViews = WeakHashMap<View?, AnimationType>()

        var countTopSlideFixedOffset: HeightCounter? = null
        var countBottomSlideFixedOffset: HeightCounter? = null

        var animateOnBackButton = true

        var delayOnStart = false
        var defaultDelay: Long = 150

        val defaultAnimationsDurations: MutableMap<AnimationType, Long> = mutableMapOf(
            AnimationType.AlphaAnimation to 300L,
            AnimationType.SlideFromTop to 225L,
            AnimationType.SlideFromBottom to 225L
        )
    }

    abstract fun handleBackButtonEventAfterAnimation()

    protected open fun animateVisibility(isVisible: Boolean,
                                    durations: Map<AnimationType, Long>? = null,
                                    delay: Long = animation.defaultDelay,
                                    endAction: () -> Unit = {}) {
        var endActionAttached = false

        val bottomFixedOffset = animation.countBottomSlideFixedOffset?.invoke()
        val topFixedOffset = animation.countTopSlideFixedOffset?.invoke()

        val longestAnimation = getLongestAnimation(durations)

        val configureAnimator = {
                animator: ViewPropertyAnimator?,
                duration: Long ->

            animator?.apply {

                this.duration = duration

                if (!endActionAttached && duration >= longestAnimation) {
                    withEndAction(endAction)
                    endActionAttached = true
                }
                this.interpolator = if (!isVisible) AccelerateInterpolator() else DecelerateInterpolator()
                startDelay = if (animation.delayOnStart || !isVisible) delay else 0
            }?.start()
        }
        val slideFromTop = animate@{ view: View?, duration: Long ->
            if (view == null)
                return@animate

            val offset = topFixedOffset ?: view.height.toFloat()
            val startValue =
                if (isVisible)
                    -offset
                else 0f
            val endValue =
                if (!isVisible)
                    -offset
                else 0f

            view.translationY = startValue

            configureAnimator(view.animate()?.translationY(endValue), duration)
        }
        val slideFromBottom = animate@{ view: View?, duration: Long ->
            if (view == null)
                return@animate

            val offset =
                bottomFixedOffset
                    ?: view.height.toFloat()
            val startValue =
                if (isVisible)
                    offset
                else 0f
            val endValue =
                if (!isVisible)
                    offset
                else 0f

            view.translationY = startValue

            configureAnimator(view.animate()?.translationY(endValue), duration)
        }
        val alphaAnimation = animate@{ view: View?, duration: Long ->

            if (view == null)
                return@animate

            val startValue = if (isVisible) 0f else 1f
            val endValue = if (!isVisible) 0f else 1f

            view.alpha = startValue

            configureAnimator(view.animate()?.alpha(endValue), duration)
        }

        for ((view, animationType) in animation.animatedViews) {
            val duration =
                durations?.get(animationType)
                    ?: animation.defaultAnimationsDurations[animationType]
                    ?: 0L
            when (animationType) {
                AnimationType.SlideFromTop -> slideFromTop(view, duration)
                AnimationType.SlideFromBottom -> slideFromBottom(view, duration)
                AnimationType.AlphaAnimation -> alphaAnimation(view, duration)
                null -> {}
            }
        }
    }

    private fun getLongestAnimation(paramDurations: Map<AnimationType, Long>?): Long {

        val presentTypes = AnimationType.values().filter {
            animation.animatedViews.containsValue(it)
        }
        var longestAnimation = paramDurations?.filter {
            presentTypes.contains(it.key)
        }?.maxBy { it.value }?.value

        val longestDefaultAnimation = animation.defaultAnimationsDurations.filter {
            presentTypes.contains(it.key)
        }.maxBy { it.value }?.value

        if (longestAnimation == null || longestDefaultAnimation ?: 0 > longestAnimation)
            longestAnimation = longestDefaultAnimation ?: 0

        return longestAnimation
    }

    override fun onResume() {
        super.onResume()
        val activity = activity
        if (activity !is BackButtonEventProvider)
            return
        activity.backButtonListener = this
    }

    override fun onPause() {
        super.onPause()
        if (!animation.animateOnBackButton)
            return

        val activity = activity
        if (activity is BackButtonEventProvider
            && activity.backButtonListener == this) {
            activity.backButtonListener = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.addSimpleOnLayoutChangeListener {
            animateVisibility(true)
        }
    }

    override fun onBackButton(): Boolean {
        if (!animation.animateOnBackButton)
            return false

        animateVisibility(false) {
            handleBackButtonEventAfterAnimation()
        }
        return true
    }
}