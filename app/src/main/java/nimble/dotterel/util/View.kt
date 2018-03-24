// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import android.view.View

val View.position: Vector2
	get()
	{
		val p = IntArray(2)
		this.getLocationOnScreen(p)
		return Vector2(p[0].toFloat(), p[1].toFloat())
	}
val View.size: Vector2
	get() = Vector2(this.width.toFloat(), this.height.toFloat())
