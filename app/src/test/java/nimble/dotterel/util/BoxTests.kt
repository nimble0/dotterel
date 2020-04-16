// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.util.*

class BoxTests : FunSpec
({
	test("in")
	{
		val box = Box(Vector2(-5f, -5f), Vector2(5f, 5f))

		(Vector2(2f, 3f) in box) shouldBe true
		(Vector2(-5f, -5f) in box) shouldBe true
		(Vector2(5f, 5f) in box) shouldBe true
		(Vector2(6f, 5f) in box) shouldBe false

		val roundedBox = RoundedBox(Vector2(-5f, -5f), Vector2(5f, 5f), 2f)

		(Vector2(-7f, 5f) in roundedBox) shouldBe true
		(Vector2(5f, 7f) in roundedBox) shouldBe true
		(Vector2(-7f, 7f) in roundedBox) shouldBe false
		(Vector2(-6f, 6f) in roundedBox) shouldBe true
	}
})
