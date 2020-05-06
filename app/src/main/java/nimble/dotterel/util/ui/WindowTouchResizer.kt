// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util.ui

import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

import kotlin.math.min

import nimble.dotterel.util.*

class WindowTouchResizer(
	private val windowManager: WindowManager,
	private val window: View,
	private val windowParams: WindowManager.LayoutParams,
	// Only allow resizing when initial touches are within this view.
	private val activationView: View,
	var minSize: Vector2,
	var maxSize: Vector2)
{
	private var resizing = false
	private var touchCorner1 = 0
	private var touchCorner2 = 0
	private var touchOffset1 = Vector2(0.0f, 0.0f)
	private var touchOffset2 = Vector2(0.0f, 0.0f)

	private val windowCorners: List<Vector2> get()
	{
		val topLeft = Vector2(this.windowParams.x.toFloat(), this.windowParams.y.toFloat())
		val bottomRight = (topLeft
			+ Vector2(this.windowParams.width.toFloat(), this.windowParams.height.toFloat()))

		return listOf(
			topLeft,
			Vector2(bottomRight.x, topLeft.y),
			Vector2(topLeft.x, bottomRight.y),
			bottomRight)
	}

	fun onTouchEvent(event: MotionEvent)
	{
		when(event.actionMasked)
		{
			MotionEvent.ACTION_DOWN,
			MotionEvent.ACTION_POINTER_DOWN ->
			{
				val activationBox = Box(
					this.activationView.screenPosition,
					this.activationView.screenPosition + this.activationView.size)
				if(event.pointerCount == 2
					&& Vector2(event.getX(0), event.getY(0)) in activationBox
					&& Vector2(event.getX(1), event.getY(1)) in activationBox)
				{
					this.resizing = true

					val touch1 = Vector2(event.getX(0), event.getY(0))
					val touch2 = Vector2(event.getX(1), event.getY(1))

					this.touchCorner1 = ((if(touch1.x < touch2.x) 0 else 1)
						+ (if(touch1.y < touch2.y) 0 else 2))
					this.touchCorner2 = this.touchCorner1 xor 3

					this.touchOffset1 = this.windowCorners[this.touchCorner1] - touch1
					this.touchOffset2 = this.windowCorners[touchCorner2] - touch2
				}
				else
					this.resizing = false
			}
			MotionEvent.ACTION_UP,
			MotionEvent.ACTION_POINTER_UP ->
				this.resizing = false
			MotionEvent.ACTION_MOVE ->
				if(this.resizing)
				{
					val touch1 = Vector2(event.getX(0), event.getY(0))
					val touch2 = Vector2(event.getX(1), event.getY(1))

					val corner1 = this.touchOffset1 + touch1
					val corner2 = this.touchOffset2 + touch2

					val topLeft = Vector2(
						if(this.touchCorner1 % 2 == 0) corner1.x else corner2.x,
						if(this.touchCorner1 / 2 == 0) corner1.y else corner2.y
					)
					val bottomRight = Vector2(
						if(this.touchCorner2 % 2 == 0) corner1.x else corner2.x,
						if(this.touchCorner2 / 2 == 0) corner1.y else corner2.y
					)

					this.resize(Box(topLeft, bottomRight))
				}
		}
	}

	private fun resize(size: Box)
	{
		val metrics = DisplayMetrics()
			.also({ this.windowManager.defaultDisplay.getMetrics(it) })
		val screenSize = Vector2(
			metrics.widthPixels.toFloat(),
			metrics.heightPixels.toFloat())

		// Limit resized dimensions to within the screen space, accounting for minimum size
		var newShape = Box(
			Vector2(
				clamp(size.topLeft.x, 0.0f, screenSize.x - minSize.x),
				clamp(size.topLeft.y, 0.0f, screenSize.y - minSize.y)
			),
			Vector2(
				clamp(size.bottomRight.x, minSize.x, screenSize.x),
				clamp(size.bottomRight.y, minSize.y, screenSize.y)
			)
		)

		// If new size is below minimum size, interpolate
		// the resize so it doesn't go below the minimum.
		val oldShape = Box(
			Vector2(this.windowParams.x.toFloat(), this.windowParams.y.toFloat()),
			Vector2(this.windowParams.x.toFloat(), this.windowParams.y.toFloat())
				+ Vector2(this.windowParams.width.toFloat(), this.windowParams.height.toFloat())
		)
		val scaleMin = (oldShape.size - this.minSize).inverseScale(oldShape.size - newShape.size)
		val scaleMax = (oldShape.size - this.maxSize).inverseScale(oldShape.size - newShape.size)
		val scale = Vector2(
			when
			{
				newShape.size.x < oldShape.size.x -> min(1.0f, scaleMin.x)
				newShape.size.x > oldShape.size.x -> min(1.0f, scaleMax.x)
				else -> 1.0f
			},
			when
			{
				newShape.size.y < oldShape.size.y -> min(1.0f, scaleMin.y)
				newShape.size.y > oldShape.size.y -> min(1.0f, scaleMax.y)
				else -> 1.0f
			}
		)
		newShape = Box(
			lerp(oldShape.topLeft, newShape.topLeft, scale),
			lerp(oldShape.bottomRight, newShape.bottomRight, scale))

		this.windowParams.x = newShape.topLeft.x.toInt()
		this.windowParams.y = newShape.topLeft.y.toInt()
		this.windowParams.width = newShape.size.x.toInt()
		this.windowParams.height = newShape.size.y.toInt()

		this.windowManager.updateViewLayout(this.window, this.windowParams)
	}
}
