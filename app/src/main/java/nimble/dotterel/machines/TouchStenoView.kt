// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

import com.eclipsesource.json.JsonObject

import kotlin.math.min

import nimble.dotterel.R
import nimble.dotterel.util.*
import nimble.dotterel.util.ui.boundingBox
import nimble.dotterel.util.ui.position

private fun MotionEvent.getTouchLine(i: Int): List<Pair<Vector2, Float>>
{
	val touchLine = mutableListOf<Pair<Vector2, Float>>()
	for(h in 0 until this.historySize)
		touchLine.add(Pair(
			Vector2(this.getHistoricalX(i, h), this.getHistoricalY(i, h)),
			this.getHistoricalPressure(i, h)))
	touchLine.add(Pair(
		Vector2(this.getX(i), this.getY(i)),
		this.getPressure(i)))

	return touchLine
}

private fun findKeyIntersections(
	line: LinearLine,
	keys: Collection<View>
): List<Pair<View, Pair<Float, Float>>>
{
	val lineBoundingBox = line.boundingBox
	return keys.filter({ lineBoundingBox.overlaps(it.boundingBox) })
		.mapNotNull({ key -> line.intersect(key.boundingBox)?.let({ Pair(key, it) }) })
		.sortedWith(compareBy({ it.second.first }, { it.second.second }))
		.filterNot({ it.second.second < 0f })
}

private fun activateKeysOnLine(line: LinearLine, keys: Collection<View>, activate: Boolean): Float
{
	val keyIntersections = findKeyIntersections(line, keys)

	var lineEnd = 0f
	for(key in keyIntersections)
	{
		if(key.second.first <= lineEnd + 1e-4f)
		{
			key.first.isSelected = activate
			lineEnd = key.second.second
		}
		else
			break
	}

	return if(keyIntersections.isEmpty()) 1f else lineEnd
}

private const val CIRCULAR_ITERATIONS = 12

class TouchStenoView(context: Context, attributes: AttributeSet) :
	StenoView(context, attributes)
{
	private class Touch(
		var position: Vector2,
		var radius: Float,
		var activate: Boolean,
		val keys: Collection<View>)
	{
		init
		{
			this.keys
				.filter({ this.position in it.boundingBox })
				.forEach({ it.isSelected = this.activate })
			this.activateNearKeys()
		}

		private fun activateNearKeys()
		{
			for(i in 0 until CIRCULAR_ITERATIONS)
			{
				val angle = (i * 2 * Math.PI / CIRCULAR_ITERATIONS).toFloat()
				activateKeysOnLine(
					LinearLine(this.position, this.position + Vector2(this.radius, 0f).rotate(angle)),
					this.keys,
					this.activate)
			}
		}

		fun update(position: Vector2, radius: Float): Boolean
		{
			val line = LinearLine(this.position, position)
			val lineEnd = min(1f, activateKeysOnLine(
				line,
				this.keys,
				this.activate))

			this.position = line.lerp(lineEnd)
			this.radius = radius

			this.activateNearKeys()

			return lineEnd < 1f
		}
	}

	private var minTouchRadius = 0f
	private var maxTouchRadius = 20f

	private val touches = mutableMapOf<Int, Touch>()

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

	override fun applyStroke()
	{
		super.applyStroke()
		this.touches.clear()
	}

	override fun dispatchTouchEvent(e: MotionEvent): Boolean
	{
		val actionI = e.actionIndex
		val pointerId = e.getPointerId(actionI)
		when(e.actionMasked)
		{
			MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
			{
				val p = Vector2(e.getX(actionI), e.getY(actionI))
				val key = this.keyAt(p)
				if(key != null)
				{
					this.touches[pointerId] = Touch(
						this.position + p,
						lerp(
							this.minTouchRadius,
							this.maxTouchRadius,
							e.getPressure(actionI)),
						!key.isSelected,
						this.keys)
					this.changeStroke()
				}
			}
			MotionEvent.ACTION_MOVE ->
			{
				val oldStroke = this.stroke
				var applyStroke = false
				for(i in 0 until e.pointerCount)
				{
					val touch = this.touches[e.getPointerId(i)] ?: continue
					for(p in e.getTouchLine(i))
					{
						val position = this.position + p.first
						val radius = lerp(
							this.minTouchRadius,
							this.maxTouchRadius,
							e.getPressure(actionI))

						if(touch.update(position, radius))
						{
							applyStroke = true
							break
						}
					}
				}
				if(this.stroke != oldStroke)
					this.changeStroke()
				// Make sure applyStroke is called after changeStroke
				if(applyStroke)
					this.applyStroke()
			}
			MotionEvent.ACTION_UP,
			MotionEvent.ACTION_POINTER_UP,
			MotionEvent.ACTION_CANCEL,
			MotionEvent.ACTION_OUTSIDE ->
			{
				val p = Vector2(e.getX(actionI), e.getY(actionI))
				if(this.touches[e.getPointerId(actionI)] != null && this.keyAt(p) == null)
					this.applyStroke()

				this.touches.remove(pointerId)
			}
		}

		super.dispatchTouchEvent(e)
		return true
	}
}
