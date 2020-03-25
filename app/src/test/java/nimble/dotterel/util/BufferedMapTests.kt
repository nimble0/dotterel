// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

class BufferedMapTests : FunSpec
({
	test("")
	{
		val entries = (1..20)
			.map({ Pair("k$it", "v$it") })
			.toMap().toMutableMap()

		val map = BufferedMap<String, String>()
		val map2 = mutableMapOf<String, String>()

		map.putAll(entries)
		map2.putAll(entries)

		map shouldBe map2

		map.startBuffering()

		for(x in 5..40 step 7)
		{
			map.remove("k$x")
			map2.remove("k$x")
		}

		map shouldBe map2

		for(x in 1..40 step 3)
		{
			map["k$x"] = "V$x"
			map2["k$x"] = "V$x"
		}

		map shouldBe map2

		for(x in 5..40 step 5)
		{
			map.remove("k$x")
			map2.remove("k$x")
		}

		map shouldBe map2

		map.flush()

		map shouldBe map2
	}
})
