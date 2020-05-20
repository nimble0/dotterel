// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util.ui

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

import nimble.dotterel.util.Box
import nimble.dotterel.util.Vector2

val View.position: Vector2
	get()
	{
		val offsetViewBounds = Rect()
		this.getDrawingRect(offsetViewBounds)
		(this.rootView as ViewGroup).offsetDescendantRectToMyCoords(this, offsetViewBounds)
		return Vector2(offsetViewBounds.left.toFloat(), offsetViewBounds.top.toFloat())
	}
val View.screenPosition: Vector2
	get()
	{
		val p = IntArray(2)
		this.getLocationOnScreen(p)
		return Vector2(p[0].toFloat(), p[1].toFloat())
	}
val View.size: Vector2
	get() = Vector2(this.width.toFloat(), this.height.toFloat())
val View.boundingBox: Box
	get() = Box(this.position, this.position + this.size)
