// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import kotlin.math.*

data class Vector2(val x: Float, val y: Float)
{
	val length: Float get() = sqrt(x*x + y*y)
	val length2: Float get() = x*x + y*y

	operator fun unaryPlus() = this
	operator fun unaryMinus() = Vector2(-this.x, -this.y)
	operator fun plus(b: Vector2) = Vector2(this.x + b.x, this.y + b.y)
	operator fun minus(b: Vector2) = Vector2(this.x - b.x, this.y - b.y)
	operator fun times(b: Float) = Vector2(this.x * b, this.y * b)
	operator fun div(b: Float) = Vector2(this.x / b, this.y / b)
	fun scale(b: Vector2) = Vector2(this.x * b.x, this.y * b.y)
	fun inverseScale(b: Vector2) = Vector2(this.x / b.x, this.y / b.y)

	fun normalised(): Vector2 = this / this.length
	fun dotProduct(b: Vector2) = this.x * b.x + this.y * b.y
	fun rotate(a: Float): Vector2
	{
		val cosA = cos(a)
		val sinA = sin(a)
		return Vector2(
			this.x * cosA - this.y * sinA,
			this.y * cosA + this.x * sinA)
	}

	companion object
	{
		val ZERO = Vector2(0f, 0f)
	}
}

fun lerp(a: Vector2, b: Vector2, interp: Float) = a + (b - a) * interp
fun lerp(a: Vector2, b: Vector2, interp: Vector2) = a + (b - a).scale(interp)

fun min(a: Vector2, b: Vector2) = Vector2(
		min(a.x, b.x),
		min(a.y, b.y)
	)
fun max(a: Vector2, b: Vector2) = Vector2(
		max(a.x, b.x),
		max(a.y, b.y)
	)
fun clamp(v: Vector2, minimum: Vector2, maximum: Vector2) = min(max(v, minimum), maximum)

fun Vector2.map(transform: (Float) -> Float) =
	Vector2(
		transform(this.x),
		transform(this.y)
	)
