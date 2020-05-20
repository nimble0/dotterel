// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

data class Box(val topLeft: Vector2, val bottomRight: Vector2)
{
	operator fun contains(p: Vector2): Boolean =
		(p.x >= this.topLeft.x 
			&& p.x <= this.bottomRight.x
			&& p.y >= this.topLeft.y
			&& p.y <= this.bottomRight.y)

	val points: List<Vector2> get() = listOf(
		this.topLeft,
		Vector2(this.bottomRight.x, this.topLeft.y),
		this.bottomRight,
		Vector2(this.topLeft.x, this.bottomRight.y))

	val size get() = this.bottomRight - this.topLeft

	fun overlaps(b: Box) = this.points.any({ it in b }) || b.points.any({ it in this })

	fun toConvexPolygon() = ConvexPolygon(listOf(
		this.topLeft,
		Vector2(this.bottomRight.x, this.topLeft.y),
		this.bottomRight,
		Vector2(this.topLeft.x, this.bottomRight.y)))

	companion object
	{
		val EMPTY = Box(Vector2.ZERO, Vector2.ZERO)
	}
}

fun Box.expand(v: Vector2) = Box(this.topLeft - v, this.bottomRight + v)
fun Box.expand(v: Float) = this.expand(Vector2(v, v))

fun LinearLine.intersect(shape: Box): Pair<Float, Float>? =
	this.intersect(shape.toConvexPolygon())
