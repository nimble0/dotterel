// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import kotlin.math.max
import kotlin.math.min

// Points should be defined in clockwise order
data class ConvexPolygon(val points: List<Vector2>)
{
	val lines: List<LinearLine>
		get()
		{
			if(this.points.isEmpty())
				return listOf()

			val lines = mutableListOf<LinearLine>()
			var lastPoint = this.points.first()
			for(point in this.points.drop(1) + listOf(lastPoint))
			{
				lines.add(LinearLine(lastPoint, point))
				lastPoint = point
			}
			return lines
		}

	operator fun contains(p: Vector2) =
		this.lines.all({
			val d = it.end - it.start
			LinearLine(p, p + Vector2(-d.y, d.x)).intersect(it) <= 0f
		})

	fun overlaps(b: ConvexPolygon): Boolean
	{
		if(this.points.isEmpty() || b.points.isEmpty())
			return false

		return b.lines.any({
				val intersection = it.intersect(this)
				intersection != null
					&& intersection.first >= 0f
					&& intersection.second <= 1f
			})
			|| b.points[0] in this
			|| this.points[0] in b
	}
}

fun ConvexPolygon.translate(v: Vector2) =
	ConvexPolygon(this.points.map({ it + v }))

fun ConvexPolygon.scale(v: Vector2) =
	ConvexPolygon(this.points.map({ it.scale(v) }))

fun ConvexPolygon.overlaps(b: Box) = b.toConvexPolygon().overlaps(this)
fun Box.overlaps(s: ConvexPolygon) = s.overlaps(this)

val ConvexPolygon.boundingBox get() =
	if(this.points.isEmpty())
		Box(Vector2(0f, 0f), Vector2(0f, 0f))
	else
		Box(
			Vector2(
				this.points.fold(Float.MAX_VALUE, { acc, it -> min(acc, it.x) }),
				this.points.fold(Float.MAX_VALUE, { acc, it -> min(acc, it.y) })),
			Vector2(
				this.points.fold(Float.MIN_VALUE, { acc, it -> max(acc, it.x) }),
				this.points.fold(Float.MIN_VALUE, { acc, it -> max(acc, it.y) }))
		)

fun LinearLine.intersect(shape: ConvexPolygon): Pair<Float, Float>?
{
	val intersections = shape.lines.mapNotNull({
		val i = this.intersect(it)
		if(it.intersect(this) in 0f..1f)
			i
		else
			null
	})

	assert(intersections.size % 2 == 0)

	return if(intersections.size >= 2)
		Pair(intersections.min()!!, intersections.max()!!)
	else
		null
}
