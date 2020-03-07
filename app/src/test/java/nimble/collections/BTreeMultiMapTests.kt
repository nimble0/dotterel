// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import kotlin.random.Random

class BTreeMultiMapTests : FunSpec
({
	val random = Random(89234)

	val map = BTreeMultiMap<String, String>(3, 8)
	val map2 = BasicMultiMap<String, String>()
	val entries = (1..1000)
		.map({ Pair("k${it * 947 % 500}", "v$it") })
		.shuffled(random)
	val sortedEntries = entries.sortedWith(compareBy({ it.first }, { it.second }))

	for(x in entries)
	{
		map.put(x.first, x.second)
		map2.put(x.first, x.second)
	}

	test("put, get")
	{
		map shouldBe map2
		for(x in entries)
			map[x.first] shouldContain x.second
		map.iterator().asSequence().map({ it.toPair() }).toList() shouldBe sortedEntries
	}

	test("remove key-value")
	{
		val shuffledEntries = entries.shuffled(random)
		val removeEntries = shuffledEntries.take(900)
		val remainingEntries = shuffledEntries.drop(removeEntries.size)
		for(x in removeEntries)
		{
			map.remove(x.first, x.second)
			map2.remove(x.first, x.second)
		}
		map shouldBe map2

		for(x in removeEntries)
			map[x.first] shouldNotContain x.second
		for(x in remainingEntries)
			map[x.first] shouldContain x.second
	}

	test("remove key")
	{
		val keys = (0..99).map({ "k$it" }).shuffled(random)
		val removeKeys = keys.take(70)
		val remainingEntries = keys
			.drop(removeKeys.size)
			.map({ Pair(it, map[it]) })

		for(x in removeKeys)
		{
			map.remove(x)
			map2.remove(x)
		}

		map shouldBe map2
		for(x in removeKeys)
			map[x].size shouldBe 0
		for(x in remainingEntries)
			map[x.first] shouldBe x.second
	}

	test("keys")
	{
		map.keys.toSet() shouldBe entries.map({ it.first }).toSet()

		map.keys.removeIf({ it.endsWith("0") })
		for(x in entries)
		{
			if(x.first.endsWith("0"))
				map[x.first].size shouldBe 0
			else
				map[x.first] shouldContain x.second
		}
	}

	test("values")
	{
		map.values.sorted() shouldBe map2.values.sorted()
		map.values.toList() shouldBe sortedEntries.map({ it.second }).toList()

		map.values.removeIf({ it.endsWith("0") })
		map2.values.removeIf({ it.endsWith("0") })

		map.values.sorted() shouldBe map2.values.sorted()
		for(x in entries)
		{
			if(x.second.endsWith("0"))
				map[x.first] shouldNotContain x.second
			else
				map[x.first] shouldContain x.second
		}
	}
})
