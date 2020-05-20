// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.widget.*

import nimble.dotterel.Dotterel
import nimble.dotterel.JsonPreferenceDataStore
import nimble.dotterel.util.*

private class FloatingDialogTouchListener(
	windowManager: WindowManager,
	window: View,
	windowParams: WindowManager.LayoutParams,
	minSize: Vector2,
	maxSize: Vector2
) :
	View.OnTouchListener
{
	private val mover = WindowTouchMover(
		windowManager,
		window,
		windowParams,
		window.findViewById(android.R.id.title))
	private val resizer = WindowTouchResizer(
		windowManager,
		window,
		windowParams,
		window,
		minSize,
		maxSize)

	override fun onTouch(view: View, event: MotionEvent): Boolean
	{
		// Coordinates relative to the view are inconsistent because we move
		// the view around. The absolute coordinates (rawX and rawY) are only
		// available for the first pointer, so we offset the relative
		// coordinates to be absolute.
		// This assumes no rotation or scaling.
		assert(view.rotation == 0.0f)
		assert(view.scaleX == 0.0f)
		assert(view.scaleY == 0.0f)
		event.offsetLocation(event.rawX - event.x,event.rawY - event.y)

		this.mover.onTouchEvent(event)
		this.resizer.onTouchEvent(event)

		return true
	}
}

open class FloatingDialog(
	protected val dotterel: Dotterel,
	layoutId: Int,
	preferenceKey: String,
	minSize: Vector2 = Vector2(100f, 100f),
	maxSize: Vector2 = Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))
{
	private val windowManager = dotterel.getSystemService(Context.WINDOW_SERVICE)
		as WindowManager

	protected val preferences = JsonPreferenceDataStore(
		dotterel.preferences!!,
		preferenceKey)
	protected val view: View = dotterel.layoutInflater
		.inflate(layoutId, null)
		.also({ it.alpha = preferences.getInt("opacity", 100) / 100.0f })

	private val windowParams: WindowManager.LayoutParams

	init
	{
		if(!dotterel.checkOverlayPermission())
			throw SecurityException("Permission to draw overlays denied.")

		val metrics = DisplayMetrics()
			.also({ this.windowManager.defaultDisplay.getMetrics(it) })

		val screenSize = Vector2(
			metrics.widthPixels.toFloat(),
			metrics.heightPixels.toFloat())

		fun dipToPixels(v: Float) =
			TypedValue
				.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP,
					v,
					metrics)
				.toInt()

		val position = clamp(
			Vector2(
				preferences.getFloat("x", 0f),
				preferences.getFloat("y", 0f)
			)
				.map({ dipToPixels(it).toFloat() }),
			Vector2.ZERO,
			screenSize - Vector2(50f, 50f)
		)
		val size = clamp(
			Vector2(
				preferences.getFloat("width", 0f),
				preferences.getFloat("height",  0f)
			)
				.map({ dipToPixels(it).toFloat() }),
			minSize.map({ dipToPixels(it).toFloat() }),
			min(maxSize.map({ dipToPixels(it).toFloat() }), screenSize)
		)

		this.windowParams = WindowManager.LayoutParams(
			size.x.toInt(),
			size.y.toInt(),
			position.x.toInt(),
			position.y.toInt(),
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
				@Suppress("DEPRECATION")
				WindowManager.LayoutParams.TYPE_PHONE
			else
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			0,
			PixelFormat.TRANSLUCENT)
		this.windowParams.gravity = Gravity.START or Gravity.TOP
		this.windowParams.title = view.findViewById<TextView>(android.R.id.title)?.text
		this.windowManager.addView(view, windowParams)

		this.view.setOnTouchListener(FloatingDialogTouchListener(
			this.windowManager,
			view,
			this.windowParams,
			minSize.map({ dipToPixels(it).toFloat() }),
			maxSize.map({ dipToPixels(it).toFloat() })
		))
	}

	open fun close()
	{
		val metrics = DisplayMetrics()
			.also({ this.windowManager.defaultDisplay.getMetrics(it) })

		fun pixelsToDip(v: Int) = v / metrics.density

		this.preferences.putFloat("width", pixelsToDip(this.windowParams.width))
		this.preferences.putFloat("height", pixelsToDip(this.windowParams.height))
		this.preferences.putFloat("x", pixelsToDip(this.windowParams.x))
		this.preferences.putFloat("y", pixelsToDip(this.windowParams.y))
		this.windowManager.removeView(this.view)
	}
}
