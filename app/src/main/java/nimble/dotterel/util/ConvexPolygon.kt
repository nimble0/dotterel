// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import android.graphics.Path

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


data class BevelData(
	val edgeLength: Float,
	val prevInterp: Float,
	val nextInterp: Float
)

fun ConvexPolygon.calculateBevels(bevels: Iterable<Float>): List<BevelData>
{
	return this.lines
		.windowedWithWrapping(2)
		.rotate(-1)
		.zip(bevels)
		.map({
			val prevLine = it.first[0].delta
			val nextLine = it.first[1].delta
			val bevel = it.second

			val prevLineLength = prevLine.length
			val nextLineLength = nextLine.length
			val edgeSize = listOf(bevel, prevLineLength, nextLineLength).min()!!

			if(edgeSize == 0f)
				BevelData(0f, 1f, 0f)
			else
			{
				val l = (prevLine / prevLineLength + nextLine / nextLineLength).length
				val s = edgeSize / l

				val interp1 = 1f - s / prevLineLength
				val interp2 = s / nextLineLength

				BevelData(s, interp1, interp2)
			}
		})
		.windowedWithWrapping(3)
		.rotate(-1)
		.map({
			val prevInterp = if(it[0].nextInterp > it[1].prevInterp)
				(it[0].nextInterp + it[1].prevInterp) / 2f
			else
				it[1].prevInterp
			val nextInterp = if(it[1].nextInterp > it[2].prevInterp)
				(it[1].nextInterp + it[2].prevInterp) / 2f
			else
				it[1].nextInterp


			BevelData(it[1].edgeLength, prevInterp, nextInterp)
		})
}

fun ConvexPolygon.calculateBevels(bevels: Sequence<Float>): List<BevelData> =
	this.calculateBevels(bevels.take(this.points.size).toList())

fun ConvexPolygon.applyBevels(bevels: List<BevelData>) =
	ConvexPolygon(this.lines
		.zip(bevels
			.windowedWithWrapping(2)
			.map({ Pair(it[0].nextInterp, it[1].prevInterp) }))
		.flatMap({
			if(it.second.first == it.second.second || it.second.second == 1f)
				listOf(it.first.lerp(it.second.first))
			else
				listOf(
					it.first.lerp(it.second.first),
					it.first.lerp(it.second.second))
		}))

fun ConvexPolygon.withBevels(bevels: Iterable<Float>): ConvexPolygon =
	this.applyBevels(this.calculateBevels(bevels))

fun ConvexPolygon.withBevels(bevels: Sequence<Float>): ConvexPolygon =
	this.withBevels(bevels.take(this.points.size).toList())


fun ConvexPolygon.toPath() =
	Path().also({
		it.moveTo(this.points.first().x, this.points.first().y)
		for(p in this.points.drop(1))
			it.lineTo(p.x, p.y)
		it.close()
	})

fun ConvexPolygon.toPathWithRoundedCorners(cornerSize: Float): Path
{
	val bevels = this.calculateBevels(generateSequence(cornerSize, { cornerSize }))

	val lines = this.lines
	val start = (lines.last().start + lines.last().end) / 2f

	val path = Path()
	path.moveTo(start.x, start.y)
	for((prevNextLines, interps) in lines.windowedWithWrapping(2).rotate(-1).zip(bevels))
	{
		val prevLine = prevNextLines[0]
		val nextLine = prevNextLines[1]

		val pointA = prevLine.lerp(interps.prevInterp)
		val pointD = nextLine.lerp(interps.nextInterp)

		val h = interps.edgeLength
		val a = cornerSize / 2f
		// Adjustment to approximate arc of a circle with cubic curve.
		// Specifically, so that the midpoint of the curves match.
		val f = (4f/3f * a / (h + a))
		val pointB = pointA + prevLine.delta * (1f - interps.prevInterp) * f
		val pointC = pointD - nextLine.delta * interps.nextInterp * f

		path.lineTo(pointA.x, pointA.y)
		path.cubicTo(
			pointB.x, pointB.y,
			pointC.x, pointC.y,
			pointD.x, pointD.y)
	}
	path.close()

	return path
}
