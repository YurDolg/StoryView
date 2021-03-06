package com.erhn.ftknft.storyview.storyview.progress

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import java.util.concurrent.TimeUnit


class StoryProgressView : View,
    IStoryProgressView {
    //colors
    @ColorInt
    private var backColor: Int = Color.DKGRAY

    @ColorInt
    private var frontColor: Int = Color.WHITE

    //paint
    private var paint: Paint = Paint()

    //bounds
    private val backBounds: RoundRectF =
        RoundRectF()

    private val frontBounds: RoundRectF =
        RoundRectF()

    //animateValue
    private var currentAnimateValue = 0f

    //animator
    private val mainAnimator = ValueAnimator.ofFloat(
        START_ANIM_VALUE,
        END_ANIM_VALUE
    )
    private var animDuration: Long = 10_000L

    // animator callbacks
    private var onStart: ((view: IStoryProgressView) -> Unit)? = null
    private var onCancel: ((view: IStoryProgressView) -> Unit)? = null
    private var onEnd: ((view: IStoryProgressView) -> Unit)? = null

    private var state: State = State.NONE

    constructor(context: Context?) : super(context) {
        initialize()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mainAnimator.addUpdateListener {
            currentAnimateValue = it.animatedValue as Float
            frontBounds.right = currentAnimateValue * backBounds.width()
            invalidate()
        }
        mainAnimator.doOnCancel {
            currentAnimateValue = 0f
            frontBounds.right = currentAnimateValue * backBounds.width()
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val actualWidth = MeasureSpec.getSize(widthMeasureSpec)
        val actualHeight = MeasureSpec.getSize(heightMeasureSpec)
        calculateBounds(actualWidth, actualHeight)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = backColor
        canvas.drawRoundRect(backBounds, backBounds.rxRound, backBounds.ryRound, paint)
        paint.color = frontColor
        canvas.drawRoundRect(frontBounds, frontBounds.rxRound, frontBounds.ryRound, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancel()
        mainAnimator.removeAllUpdateListeners()
    }


    override fun setFrontColor(color: Int) {
        frontColor = color
    }

    override fun setBackColor(color: Int) {
        backColor = color
    }

    override fun setDuration(duration: Long, timeUnit: TimeUnit) {
        animDuration = timeUnit.toMillis(duration)
        cancel()
        initialize()
        invalidate()
    }

    override fun doOnStart(action: (view: IStoryProgressView) -> Unit) {
        onStart = action
        mainAnimator.doOnStart { onStart?.invoke(this) }
    }

    override fun doOnEnd(action: (view: IStoryProgressView) -> Unit) {
        onEnd = action
        mainAnimator.doOnEnd {
            if (state != State.CANCEL) {
                state = State.END
                onEnd?.invoke(this)
            }
        }
    }

    override fun doOnCancel(action: (view: IStoryProgressView) -> Unit) {
        onCancel = action
        mainAnimator.doOnCancel {
            onCancel?.invoke(this)
        }
    }

    override fun currentProgress(): Int {
        return Math.round(currentAnimateValue * 100)
    }

    override fun start() {
        state = State.PLAY
        mainAnimator.start()
    }

    override fun cancel() {
        state = State.CANCEL
        mainAnimator.cancel()
    }

    override fun pause() {
        state = State.PAUSE
        mainAnimator.pause()
    }

    override fun resume() {
        state = State.PLAY
        mainAnimator.resume()
    }

    override fun end() {
    }

    override fun clearProgress() {
        currentAnimateValue = START_ANIM_VALUE
        frontBounds.right = currentAnimateValue * backBounds.right
        invalidate()
    }

    override fun fillInProgress() {
        currentAnimateValue = END_ANIM_VALUE
        frontBounds.right = currentAnimateValue * backBounds.right
        invalidate()
    }

    private fun initialize() {
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.isAntiAlias = true

        mainAnimator.setDuration(animDuration)
        mainAnimator.interpolator = LinearInterpolator()
    }

    private fun calculateBounds(actualWidth: Int, actualHeight: Int) {
        backBounds.apply {
            left = paddingStart.toFloat()
            right = actualWidth - paddingEnd.toFloat()
            top = paddingTop.toFloat()
            bottom = actualHeight - paddingBottom.toFloat()
            rxRound = this.height() / 2f
            ryRound = this.height() / 2f
        }

        frontBounds.apply {
            left = backBounds.left
            right = backBounds.right * currentAnimateValue
            top = paddingTop.toFloat()
            bottom = actualHeight - paddingBottom.toFloat()
            rxRound = backBounds.rxRound
            ryRound = backBounds.ryRound
        }

    }

    enum class State {
        PLAY, END, CANCEL, PAUSE, NONE
    }

    companion object {
        private const val START_ANIM_VALUE = 0f
        private const val END_ANIM_VALUE = 1f
    }
}