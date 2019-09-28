// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import kotlin.math.max
import kotlin.math.min

data class LinearLine(val start: Vector2, val end: Vector2)
{
	operator fun plus(b: Vector2) = LinearLine(this.start + b, this.end + b)

	val delta: Vector2 get() = this.end - this.start

	fun intersect(b: LinearLine): Float
	{
		// c = this.start
		// d = this.end
		// e = b.start
		// f = b.end
		val ec = this.start - b.start
		val dc = this.end - this.start
		val fe = b.end - b.start

		return (ec.x * fe.y - ec.y * fe.x) / (fe.x * dc.y - fe.y * dc.x)
	}

	fun lerp(interp: Float) = this.start + (this.end - this.start) * interp
}

val LinearLine.boundingBox get() =
	Box(
		Vector2(
			min(this.start.x, this.end.x),
			min(this.start.y, this.end.y)),
		Vector2(
			max(this.start.x, this.end.x),
			max(this.start.y, this.end.y))
	)
