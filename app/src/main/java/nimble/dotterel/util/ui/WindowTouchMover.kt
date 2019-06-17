// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util.ui

import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

import nimble.dotterel.util.*

class WindowTouchMover(
	private val windowManager: WindowManager,
	private val window: View,
	private val windowParams: WindowManager.LayoutParams,
	// Only allow moving when initial touch is within this view.
	private val activationView: View)
{
	private var moving = false
	private var offset = Vector2(0.0f, 0.0f)

	fun onTouchEvent(event: MotionEvent)
	{
		when(event.actionMasked)
		{
			MotionEvent.ACTION_DOWN,
			MotionEvent.ACTION_POINTER_DOWN ->
			{
				val activationBox = Box(
					this.activationView.position,
					this.activationView.position + this.activationView.size)
				if(event.pointerCount == 1 && Vector2(event.x, event.y) in activationBox)
				{
					this.moving = true
					this.offset = Vector2(
						this.windowParams.x.toFloat(),
						this.windowParams.y.toFloat()
					) - Vector2(event.x, event.y)
				}
				else
					this.moving = false
			}
			MotionEvent.ACTION_UP,
			MotionEvent.ACTION_POINTER_UP ->
				this.moving = false
			MotionEvent.ACTION_MOVE ->
				if(this.moving)
					this.move(Vector2(event.rawX, event.rawY) + this.offset)
		}
	}

	private fun move(p: Vector2)
	{
		val metrics = DisplayMetrics()
			.also({ this.windowManager.defaultDisplay.getMetrics(it) })

		this.windowParams.x = clamp(
			p.x.toInt(),
			0,
			metrics.widthPixels - this.windowParams.width)
		this.windowParams.y = clamp(
			p.y.toInt(),
			0,
			metrics.heightPixels - this.windowParams.height)

		this.windowManager.updateViewLayout(this.window, this.windowParams)
	}
}
