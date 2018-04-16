// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import io.kotlintest.properties.forAll
import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.Gen
import io.kotlintest.specs.FunSpec

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

fun Float.delta(d: Float) = (this - d).rangeTo(this + d)

class GeometryTests : FunSpec
({
	test("box in")
	{
		val box = Box(Vector2(-5f, -5f), Vector2(5f, 5f))

		(Vector2(2f, 3f) in box) shouldBe true
		(Vector2(-5f, -5f) in box) shouldBe true
		(Vector2(5f, 5f) in box) shouldBe true
		(Vector2(6f, 5f) in box) shouldBe false
	}

	test("box overlaps")
	{
		val box = Box(Vector2(-5f, -5f), Vector2(5f, 5f))

		box.overlaps(box) shouldBe true
		box.overlaps(Box(Vector2(-2f, -2f), Vector2(2f, 2f))) shouldBe true
		box.overlaps(Box(Vector2(-10f, -10f), Vector2(10f, 10f))) shouldBe true
		box.overlaps(Box(Vector2(0f, 0f), Vector2(10f, 10f))) shouldBe true
		box.overlaps(Box(Vector2(-10f, 10f), Vector2(10f, 12f))) shouldBe false
		box.overlaps(Box(Vector2(10f, -10f), Vector2(12f, 10f))) shouldBe false
	}

	test("linear line intersect")
	{
		val random = Random(239408)

		class LinearLineGenerator : Gen<LinearLine>
		{
			override fun generate() =
				LinearLine(
					Vector2(
						random.nextFloat(),
						random.nextFloat()),
					Vector2(
						random.nextFloat(),
						random.nextFloat())
				)
		}

		val a = LinearLine(Vector2(0f, 0f), Vector2(10f, 0f))
		val b = LinearLine(Vector2(2f, -2f), Vector2(2f, 2f))
		a.intersect(b) shouldBe 0.2f
		b.intersect(a) shouldBe 0.5f

		@Suppress("NAME_SHADOWING")
		forAll(LinearLineGenerator(), LinearLineGenerator(),
			{ a: LinearLine, b: LinearLine ->
				val intersect1 = a.intersect(b)
				val intersect2 = LinearLine(a.end, a.start).intersect(b)
				intersect1 in (1 - intersect2).delta(max(1f, abs(intersect1)) * 1e-4f)
			})
	}

	test("box-linear line intersect")
	{
		val box = Box(Vector2(-5f, -5f), Vector2(5f, 5f))

		LinearLine(Vector2(0f, 0f), Vector2(1f, 1f))
			.intersect(box) shouldBe Pair(-5f, 5f)
		LinearLine(Vector2(0f, 0f), Vector2(8f, 1f))
			.intersect(box) shouldBe Pair(-0.625f, 0.625f)
		LinearLine(Vector2(8f, 1f), Vector2(0f, 0f))
			.intersect(box) shouldBe Pair(0.375f, 1.625f)
		LinearLine(Vector2(0f, 11f), Vector2(11f, 0f))
			.intersect(box) shouldBe null
		LinearLine(Vector2(10f, 11f), Vector2(0f, 1f))
			.intersect(box) shouldBe Pair(0.6f, 1.5f)
	}

	test("convex shape in")
	{
		val random = Random(239408)

		class BoxGenerator : Gen<Box>
		{
			override fun generate(): Box
			{
				val topLeft = Vector2(
					random.nextFloat(),
					random.nextFloat())
				val size = Vector2(
					random.nextFloat(),
					random.nextFloat())
				return Box(topLeft, topLeft + size)
			}
		}

		class Vector2Generator : Gen<Vector2>
		{
			override fun generate() = Vector2(random.nextFloat(), random.nextFloat())
		}

		@Suppress("NAME_SHADOWING")
		forAll(BoxGenerator(), Vector2Generator(),
			{ a: Box, b: Vector2 ->
				if(b in a != b in a.toConvexPolygon())
				{
					println(a)
					println(b)
				}

				b in a == b in a.toConvexPolygon()
			})
	}
})
