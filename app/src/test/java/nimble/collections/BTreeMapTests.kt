// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import kotlin.random.Random

class BTreeMapTests : FunSpec
({
	val random = Random(89234)

	val map = BTreeMap<String, String>(3, 8)
	val map2 = mutableMapOf<String, String>()
	val entries = (1..1000)
		.map({ Pair("k${it * 947 % 500}", "v$it") })
		.shuffled(random)

	test("put, get")
	{
		for(x in entries)
		{
			map[x.first] = x.second
			map2[x.first] = x.second
		}
		map shouldBe map2
		for(x in map2)
			map[x.key] shouldBe x.value
		for(x in map)
			map2[x.key] shouldBe x.value
	}

	test("putIfAbsent, get")
	{
		for(x in entries)
		{
			map.putIfAbsent(x.first, x.second)
			map2.putIfAbsent(x.first, x.second)
		}
		map shouldBe map2
	}

	test("add")
	{
		for(x in entries)
		{
			map.add(BTreeMap.Entry(x.first, x.second))
			map2[x.first] = x.second
		}
		map shouldBe map2
	}

	test("keys")
	{
		for(x in entries)
		{
			map[x.first] = x.second
			map2[x.first] = x.second
		}

		map.keys.removeIf({ it.endsWith("0") })
		map2.keys.removeIf({ it.endsWith("0") })

		map shouldBe map2
	}

	test("values")
	{
		for(x in entries)
		{
			map[x.first] = x.second
			map2[x.first] = x.second
		}

		map.values.removeIf({ it.endsWith("0") })
		map2.values.removeIf({ it.endsWith("0") })

		map shouldBe map2
	}
})
