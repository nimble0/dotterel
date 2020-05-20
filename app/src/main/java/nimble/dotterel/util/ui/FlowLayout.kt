// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

import kotlin.math.*

class FlowLayout(context: Context, attributes: AttributeSet) :
	ViewGroup(context, attributes)
{
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
	{
		val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)

		val maxX = if(widthMode == View.MeasureSpec.UNSPECIFIED)
			-1
		else
			View.MeasureSpec.getSize(widthMeasureSpec) - this.paddingRight

		var width = 0

		var x = this.paddingLeft
		var y = this.paddingTop

		var lineHeight = 0
		for(i in 0 until this.childCount)
		{
			val child = getChildAt(i)
			this.measureChild(child, widthMeasureSpec, heightMeasureSpec)

			if(maxX != -1
				&& x + child.measuredWidth > maxX)
			{
				x = this.paddingLeft
				y += lineHeight
				lineHeight = 0
			}

			(child.layoutParams as LayoutParams).apply({
				this.x = x
				this.y = y
			})

			x += child.measuredWidth
			lineHeight = max(lineHeight, child.measuredHeight)

			width = max(width, x)
		}

		width += paddingRight
		val height = y + lineHeight + paddingBottom

		this.setMeasuredDimension(
			View.resolveSize(width, widthMeasureSpec),
			View.resolveSize(height, heightMeasureSpec))
	}

	override fun onLayout(
		changed: Boolean,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int)
	{
		for(i in 0 until this.childCount)
		{
			val child = getChildAt(i)
			val layoutParams = child.layoutParams as LayoutParams
			child.layout(
				layoutParams.x,
				layoutParams.y,
				layoutParams.x + child.measuredWidth,
				layoutParams.y + child.measuredHeight)
		}
	}

	override fun checkLayoutParams(params: ViewGroup.LayoutParams) =
		params is LayoutParams

	override fun generateDefaultLayoutParams() =
		LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT)

	override fun generateLayoutParams(attributes: AttributeSet) =
		LayoutParams(this.context, attributes)

	override fun generateLayoutParams(params: ViewGroup.LayoutParams) =
		LayoutParams(params.width, params.height)

	class LayoutParams : ViewGroup.LayoutParams
	{
		var x: Int = 0
		var y: Int = 0

		constructor(context: Context, attributes: AttributeSet) :
			super(context, attributes)
		constructor(w: Int, h: Int) : super(w, h)
	}
}
