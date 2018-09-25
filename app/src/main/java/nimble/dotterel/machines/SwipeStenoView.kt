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
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView

import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.util.Vector2

private fun MotionEvent.getTouchLine(i: Int): List<Vector2>
{
	val touchLine = mutableListOf<Vector2>()
	for(h in 0 until this.historySize)
		touchLine.add(Vector2(this.getHistoricalX(i, h), this.getHistoricalY(i, h)))
	touchLine.add(Vector2(this.getX(i), this.getY(i)))

	return touchLine
}

class SwipeStenoView(context: Context, attributes: AttributeSet) :
	StenoView(context, attributes)
{
	override val keyLayout = KeyLayout("#STKPWHR-AO*EU-FRPBLGTSDZ")

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
}
