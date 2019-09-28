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
}

fun Box.expand(v: Vector2) = Box(this.topLeft - v, this.bottomRight + v)
fun Box.expand(v: Float) = this.expand(Vector2(v, v))

fun LinearLine.intersect(shape: Box): Pair<Float, Float>? =
	this.intersect(shape.toConvexPolygon())

data class RoundedBox(val topLeft: Vector2, val bottomRight: Vector2, val radius: Float)
{
	private val radius2 = radius * radius

	operator fun contains(p: Vector2): Boolean
	{
		val topRight = Vector2(this.bottomRight.x, this.topLeft.y)
		val bottomLeft = Vector2(this.topLeft.x, this.bottomRight.y)

		return ((p.x >= this.topLeft.x - this.radius
			&& p.x <= this.bottomRight.x + this.radius
			&& p.y >= this.topLeft.y
			&& p.y <= this.bottomRight.y)
			|| (p.y >= this.topLeft.y - this.radius
			&& p.y <= this.bottomRight.y + this.radius
			&& p.x >= this.topLeft.x
			&& p.x <= this.bottomRight.x)
			|| (p - this.topLeft).length2 <= this.radius2
			|| (p - topRight).length2 <= this.radius2
			|| (p - bottomLeft).length2 <= this.radius2
			|| (p - this.bottomRight).length2 <= this.radius2)
	}
}
