package ly.slide.promo.testapp.presentation.widget

import android.animation.Animator
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import ly.slide.promo.testapp.domain.entity.Media
import ly.slide.promo.testapp.presentation.UiConstants
import ly.slide.promo.testapp.presentation.util.alsoSaveState
import ly.slide.promo.testapp.presentation.util.restoreSavedState
import ly.slide.promo.testapp.presentation.widget.slide.ImageSlideView
import ly.slide.promo.testapp.presentation.widget.slide.Slide
import ly.slide.promo.testapp.presentation.widget.slide.VideoSlideView
import kotlin.properties.Delegates

class SliderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val STATE_ANIMATION_DURATION = "state:animationDuration"
    }

    var animationDuration: Long by Delegates.vetoable(
            initialValue = UiConstants.ANIMATION_DURATION_DEFAULT,
            onChange = { _, oldValue, newValue ->
                newValue != oldValue && newValue in LongRange(
                        UiConstants.ANIMATION_DURATION_MIN,
                        UiConstants.ANIMATION_DURATION_MAX
                )
            }
    )

    private var previousSlide: View? = null
    private var currentSlide: View? = null
    private var nextSlide: View? = null

    fun showSlide(media: Media?) {
        previousSlide = currentSlide
        currentSlide = nextSlide
        nextSlide = null
        when {
            currentSlide?.tag == media -> {
                (currentSlide as? Slide)?.start()
                currentSlide?.animate()
                        ?.alpha(1f)
                        ?.setDuration(animationDuration)
                        ?.setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animator: Animator) = Unit
                            override fun onAnimationRepeat(animator: Animator) = Unit
                            override fun onAnimationEnd(animator: Animator) = removeSlide(previousSlide)
                            override fun onAnimationCancel(animator: Animator) = removeSlide(previousSlide)
                        })
                        ?.start()
            }
            media == null -> currentSlide = null
            currentSlide?.bindMedia(media) != true -> {
                currentSlide = createSlide(media.type).apply {
                    bindMedia(media)
                    (this as? VideoSlideView)?.start()
                    addSlide(this, visible = true)
                }
            }
        }
    }

    fun prepareSlide(media: Media?) {
        nextSlide = media?.let {
            createSlide(media.type).apply {
                bindMedia(media)
                addSlide(this, visible = false)
            }
        }
    }

    private fun createSlide(type: Media.Type): View {
        return when (type) {
            Media.Type.IMAGE -> ImageSlideView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            Media.Type.VIDEO -> VideoSlideView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    }

    private fun View.bindMedia(media: Media?): Boolean {
        tag = media
        when {
            media == null -> return false
            media.type == Media.Type.IMAGE && this is ImageSlideView -> prepare(media.uri)
            media.type == Media.Type.VIDEO && this is VideoSlideView -> prepare(media.uri)
            else -> return false
        }
        return true
    }

    private fun removeSlide(view: View?) {
        view?.let(::removeView)
    }

    private fun addSlide(view: View?, visible: Boolean) {
        view?.apply { alpha = if (visible) 1f else 0f }?.let(::addView)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return superState.alsoSaveState(
                STATE_ANIMATION_DURATION to animationDuration
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val superState = state.restoreSavedState {
            animationDuration = getLong(STATE_ANIMATION_DURATION)
        }
        super.onRestoreInstanceState(superState)
    }
}