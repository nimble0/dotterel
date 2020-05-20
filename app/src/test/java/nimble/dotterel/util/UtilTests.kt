// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

class UtilTests : FunSpec
({
	test("product")
	{
		product(listOf(2, 5, 3)).toList() shouldBe listOf(
			listOf(0, 0, 0),
			listOf(0, 0, 1),
			listOf(0, 0, 2),
			listOf(0, 1, 0),
			listOf(0, 1, 1),
			listOf(0, 1, 2),
			listOf(0, 2, 0),
			listOf(0, 2, 1),
			listOf(0, 2, 2),
			listOf(0, 3, 0),
			listOf(0, 3, 1),
			listOf(0, 3, 2),
			listOf(0, 4, 0),
			listOf(0, 4, 1),
			listOf(0, 4, 2),
			listOf(1, 0, 0),
			listOf(1, 0, 1),
			listOf(1, 0, 2),
			listOf(1, 1, 0),
			listOf(1, 1, 1),
			listOf(1, 1, 2),
			listOf(1, 2, 0),
			listOf(1, 2, 1),
			listOf(1, 2, 2),
			listOf(1, 3, 0),
			listOf(1, 3, 1),
			listOf(1, 3, 2),
			listOf(1, 4, 0),
			listOf(1, 4, 1),
			listOf(1, 4, 2)
		)
	}
})
