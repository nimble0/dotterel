// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

import com.eclipsesource.json.JsonObject

import nimble.dotterel.R
import nimble.dotterel.util.*
import nimble.dotterel.util.ui.position
import nimble.dotterel.util.ui.size

import kotlin.math.*

private const val APPLY_STROKE_DISTANCE = 100f

class TouchStenoView(context: Context, attributes: AttributeSet) :
	StenoView(context, attributes)
{
	private val touches = mutableMapOf<Int, Touch>()

	private class Touch(
		val start: Vector2,
		val keyPress: Boolean,
		val action: Boolean,
		size: Float)
	{
		var largestSize: Float = size
			private set
		var largestDistance2: Float = 0f
			private set

		fun process(e: MotionEvent, i: Int)
		{
			for(h in 0 until e.historySize)
			{
				val p = Vector2(e.getHistoricalX(i, h), e.getHistoricalY(i, h))
				this.largestSize = max(
					this.largestSize,
					e.getHistoricalPressure(i, h))
				this.largestDistance2 = max(
					this.largestDistance2,
					(this.start - p).length2)
			}

			val p = Vector2(e.getX(i), e.getY(i))
			this.largestSize = max(this.largestSize, e.getSize(i))
			this.largestDistance2 = max(this.largestDistance2, (start - p).length2)
		}
	}

	private var minTouchRadius = 0f
	private var maxTouchRadius = 20f

	override fun setConfig(config: JsonObject, systemConfig: JsonObject)
	{
		fun dipToPixels(v: Float) =
			TypedValue
				.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP,
					v,
					context.resources.displayMetrics)

		this.minTouchRadius = dipToPixels(config.get("minTouchRadius").asFloat())
		this.maxTouchRadius = dipToPixels(config.get("maxTouchRadius").asFloat())
		val padding = dipToPixels(config.get("padding").asFloat()).toInt()
		this.findViewById<View>(R.id.keys).setPadding(padding, padding, padding, padding)
	}

	override fun onFinishInflate()
	{
		super.onFinishInflate()
		this.translationPreview?.setOnClickListener({ this.applyStroke() })
	}

	private fun setKeysNear(p: Vector2, radius: Float, select: Boolean)
	{
		this.keys
			.filter({ (this.position + p) in
				RoundedBox(it.position, it.position + it.size, radius) })
			.forEach({ it.isSelected = select })
		this.changeStroke()
	}

	private fun setKeysNear(touch: Touch)
	{
		if(touch.keyPress)
			setKeysNear(
				touch.start,
				lerp(this.minTouchRadius, this.maxTouchRadius, touch.largestSize),
				touch.action)
	}

	override fun applyStroke()
	{
		super.applyStroke()
		this.touches.clear()
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(e: MotionEvent): Boolean
	{
		val actionI = e.actionIndex
		val pointerId = e.getPointerId(actionI)
		when(e.actionMasked)
		{
			MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
			{
				val p = Vector2(e.getX(actionI), e.getY(actionI))
				val key = this.keyAt(p)
				val touch = Touch(
					p,
					// Touch doesn't activate or deactivate keys if no key is
					// directly hit, but it can apply the stroke.
					key != null,
					key != null && !key.isSelected,
					e.getSize(actionI))
				this.touches[pointerId] = touch
				this.setKeysNear(touch)
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
			{
				this.touches.remove(pointerId)
				this.performClick()
			}
			MotionEvent.ACTION_MOVE ->
			{
				var applyStroke = false
				for(i in 0 until e.pointerCount)
				{
					val touch = this.touches[e.getPointerId(i)] ?: continue

					val pressure = touch.largestSize
					touch.process(e, i)
					if(touch.largestSize > pressure)
						this.setKeysNear(touch)

					if(touch.largestDistance2
						> APPLY_STROKE_DISTANCE * APPLY_STROKE_DISTANCE)
						applyStroke = true
				}

				if(applyStroke)
					this.applyStroke()
			}
		}

		return true
	}
}
