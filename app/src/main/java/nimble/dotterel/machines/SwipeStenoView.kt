// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

// Copyright (c) 2018 Brent Nesbitt
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package nimble.dotterel.machines

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.TextView

import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat

import kotlin.math.*

import nimble.dotterel.R
import nimble.dotterel.util.*

private fun MotionEvent.getTouchLine(i: Int): List<Vector2>
{
	val touchLine = mutableListOf<Vector2>()
	for(h in 0 until this.historySize)
		touchLine.add(Vector2(this.getHistoricalX(i, h), this.getHistoricalY(i, h)))
	touchLine.add(Vector2(this.getX(i), this.getY(i)))

	return touchLine
}

private class SwipeKeyDrawable(
	private val bevels: List<Boolean>,
	private val bevelSize: Float,
	private val strokePaint: Paint,
	private val fillPaint: Paint,
	private val cornerSize: Float
) :
	Drawable()
{
	var shape: ConvexPolygon = ConvexPolygon(listOf())
		private set

	override fun setAlpha(alpha: Int) = Unit
	override fun setColorFilter(colorFilter: ColorFilter?) = Unit
	override fun getOpacity(): Int = PixelFormat.UNKNOWN

	override fun draw(canvas: Canvas)
	{
		val canvasBounds = Box(
			Vector2(this.bounds.left.toFloat(), this.bounds.top.toFloat()),
			Vector2(this.bounds.right.toFloat(), this.bounds.bottom.toFloat()))

		val bevelSize = this.bevelSize * min(canvasBounds.size.x, canvasBounds.size.y)
		this.shape = canvasBounds
			.toConvexPolygon()
			.withBevels((this.bevels.map({ if(it) bevelSize else 0f })
				+ List(max(0, canvasBounds.points.size - this.bevels.size), { 0f })))

		val path = this.shape.toPathWithRoundedCorners(this.cornerSize)
		canvas.drawPath(path, this.fillPaint)
		canvas.drawPath(path, this.strokePaint)
	}
}

private fun swipeKeyDrawable(
	context: Context,
	bevels: List<Boolean>,
	bevelSize: Float,
	fillColour: Int,
	strokeColour: Int,
	selectedFillColour: Int,
	selectedStrokeColour: Int
) =
	StateListDrawable().also({ stateList ->
		val strokePaint = Paint().also({
			it.style = Paint.Style.STROKE
			it.strokeWidth = TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP,
				1f,
				context.resources.displayMetrics)
			it.isAntiAlias = true
		})
		val fillPaint = Paint().also({
			it.style = Paint.Style.FILL
		})
		val cornerSize = TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP,
			10f,
			context.resources.displayMetrics)

		stateList.addState(
			intArrayOf(android.R.attr.state_activated),
			SwipeKeyDrawable(
				bevels,
				bevelSize,
				Paint(strokePaint).also({ it.color = selectedStrokeColour }),
				Paint(fillPaint).also({ it.color = selectedFillColour }),
				cornerSize
			))
		stateList.addState(
			intArrayOf(android.R.attr.state_selected),
			SwipeKeyDrawable(
				bevels,
				bevelSize,
				Paint(strokePaint).also({ it.color = selectedStrokeColour }),
				Paint(fillPaint).also({ it.color = selectedFillColour }),
				cornerSize
			))
		stateList.addState(
			intArrayOf(),
			SwipeKeyDrawable(
				bevels,
				bevelSize,
				Paint(strokePaint).also({ it.color = strokeColour }),
				Paint(fillPaint).also({ it.color = fillColour }),
				cornerSize
			))
	})

class SwipeStenoKey(
	context: Context,
	attrs: AttributeSet
) :
	AppCompatTextView(context, attrs)
{
	var bevels: List<Boolean>
	var specialKey = false
	var bevelSize: Float = 0f
		set(v)
		{
			field = v
			this.setBackgroundResource(0)
			this.background = if(this.specialKey)
			{
				swipeKeyDrawable(
					this.context,
					this.bevels,
					this.bevelSize,
					ContextCompat.getColor(this.context, R.color.specialStenoKey),
					ContextCompat.getColor(this.context, R.color.specialStenoKeyBorder),
					ContextCompat.getColor(this.context, R.color.specialSelectedStenoKey),
					ContextCompat.getColor(this.context, R.color.specialSelectedStenoBorder))
			}
			else
			{
				swipeKeyDrawable(
					this.context,
					this.bevels,
					this.bevelSize,
					ContextCompat.getColor(this.context, R.color.stenoKey),
					ContextCompat.getColor(this.context, R.color.stenoKeyBorder),
					ContextCompat.getColor(this.context, R.color.selectedStenoKey),
					ContextCompat.getColor(this.context, R.color.selectedStenoBorder))
			}
		}

	init
	{
		val attributes = context.obtainStyledAttributes(attrs, R.styleable.SwipeStenoKey, 0, 0)

		val bevelTopLeft = attributes.getBoolean(R.styleable.SwipeStenoKey_bevelTopLeft, false)
		val bevelTopRight = attributes.getBoolean(R.styleable.SwipeStenoKey_bevelTopRight, false)
		val bevelBottomLeft = attributes.getBoolean(R.styleable.SwipeStenoKey_bevelBottomLeft, false)
		val bevelBottomRight = attributes.getBoolean(R.styleable.SwipeStenoKey_bevelBottomRight, false)
		this.bevels = listOf(bevelTopLeft, bevelTopRight, bevelBottomRight, bevelBottomLeft)
		this.specialKey = attributes.getBoolean(R.styleable.SwipeStenoKey_specialKey, false)

		attributes.recycle()
	}
}

class SwipeStenoView(context: Context, attributes: AttributeSet) :
	StenoView(context, attributes)
{
	private val touches = mutableMapOf<Int, Vector2>()

	override fun applyStroke()
	{
		super.applyStroke()
		this.touches.clear()
	}

	private fun toggleKeys(start: Vector2, points: List<Vector2>)
	{
		var lastKey: TextView? = this.keyAt(start)
		for(p in points)
		{
			val key = this.keyAt(p)

			if(key != lastKey && key != null)
				key.isSelected = !key.isSelected

			lastKey = key
		}
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
				this.touches[pointerId] = p

				val key = this.keyAt(p)
				if(key != null)
					key.isSelected = !key.isSelected
			}
			MotionEvent.ACTION_MOVE ->
			{
				for(i in 0 until e.pointerCount)
				{
					val touch = this.touches[e.getPointerId(i)] ?: continue
					val touchLine = e.getTouchLine(i)

					this.toggleKeys(touch, touchLine)

					this.touches[e.getPointerId(i)] = Vector2(e.getX(i), e.getY(i))
				}
				this.changeStroke()
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
			{
				this.touches.remove(pointerId)
				if(this.touches.isEmpty())
					this.applyStroke()
			}
		}
		return true
	}

	override fun onFinishInflate()
	{
		super.onFinishInflate()

		for(key in this.keys)
			(key as? SwipeStenoKey)?.bevelSize = 0.6f
	}
}
