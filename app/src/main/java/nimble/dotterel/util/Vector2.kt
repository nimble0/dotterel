// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import kotlin.math.sqrt

class Vector2(val x: Float, val y: Float)
{
	val length: Float get() = sqrt(x*x + y*y)
	val length2: Float get() = x*x + y*y

	operator fun unaryPlus() = this
	operator fun unaryMinus() = Vector2(-this.x, -this.y)
	operator fun plus(b: Vector2) = Vector2(this.x + b.x, this.y + b.y)
	operator fun minus(b: Vector2) = Vector2(this.x - b.x, this.y - b.y)
	operator fun times(b: Float) = Vector2(this.x * b, this.y * b)
	operator fun div(b: Float) = Vector2(this.x / b, this.y / b)
}
