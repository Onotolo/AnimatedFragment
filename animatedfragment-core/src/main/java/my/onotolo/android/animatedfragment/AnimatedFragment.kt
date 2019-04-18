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

        var defaultDuration: Long = 225
        var defaultDelay: Long = 150
    }

    abstract fun handleBackButtonEventAfterAnimation()

    protected open fun animateVisibility(isVisible: Boolean,
                                    duration: Long = animation.defaultDuration,
                                    delay: Long = animation.defaultDelay,
                                    endAction: () -> Unit = {}) {
        var endActionAttached = false

        val bottomFixedOffset = animation.countBottomSlideFixedOffset?.invoke()
        val topFixedOffset = animation.countTopSlideFixedOffset?.invoke()

        val configureAnimator = { animator: ViewPropertyAnimator? ->
            animator?.apply {

                this.duration = duration

                if (!endActionAttached) {
                    withEndAction(endAction)
                    endActionAttached = true
                }
                this.interpolator = if (!isVisible) AccelerateInterpolator() else DecelerateInterpolator()
                startDelay = delay
            }?.start()
        }
        val slideFromTop = animate@{ view: View? ->
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

            configureAnimator(view.animate()?.translationY(endValue))
        }
        val slideFromBottom = animate@{ view: View? ->
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

            configureAnimator(view.animate()?.translationY(endValue))
        }
        val alphaAnimation = animate@{ view: View? ->

            if (view == null)
                return@animate

            val startValue = if (isVisible) 0f else 1f
            val endValue = if (!isVisible) 0f else 1f

            view.alpha = startValue

            configureAnimator(view.animate()?.alpha(endValue))
        }

        for ((view, position) in animation.animatedViews) {
            when (position) {
                AnimationType.SlideFromTop -> slideFromTop(view)
                AnimationType.SlideFromBottom -> slideFromBottom(view)
                AnimationType.AlphaAnimation -> alphaAnimation(view)
                null -> {}
            }
        }
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

        animateVisibility(false, 175) {
            handleBackButtonEventAfterAnimation()
        }
        return true
    }
}