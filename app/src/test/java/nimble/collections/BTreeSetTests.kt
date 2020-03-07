// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import kotlin.random.Random

fun <T> BTreeSet<T>.isOrdered(): Boolean
{
	var prev: T? = null
	for(x in this)
	{
		val prev2 = prev
		if(prev2 != null && this.compare(prev2, x) > 0)
			return false
		prev = x
	}

	return true
}

class BTreeSetTests : FunSpec
({
	val random = Random(89234)

	val set = BTreeSet<String>(3, 8, { a, b -> a.compareTo(b) })
	val entries = (1..1000)
		.map({ "$it" })
		.shuffled(random)
	val sortedEntries = entries.sorted()

	test("add, get")
	{
		val set2 = mutableSetOf<String>()
		for(x in entries)
		{
			set.add(x)
			set2.add(x)
		}

		for(x in set)
			set2 shouldContain x
		for(x in set2)
			set shouldContain x

		val nonEntries = (1001..2000).map({ "$it" })
		for(x in nonEntries)
			set shouldNotContain x

		set.isOrdered() shouldBe true
	}

	test("iterator")
	{
		for(x in entries.shuffled(random))
			set.add(x)

		val iter = set.iterator()
		val nextEntries = mutableListOf<String>()
		while(iter.hasNext())
			nextEntries.add(iter.next())

		nextEntries shouldBe sortedEntries
	}

	test("remove")
	{
		for(x in entries.shuffled(random))
			set.add(x)

		val shuffledEntries = entries.shuffled(random)
		val removeEntries = shuffledEntries.take(900)
		val remainingEntries = shuffledEntries.drop(removeEntries.size)
		for(x in removeEntries)
			set.remove(x)
		for(x in removeEntries)
			set shouldNotContain x
		for(x in remainingEntries)
			set shouldContain x

		for(x in remainingEntries)
			set.remove(x)
		set.iterator().hasNext() shouldBe false

		set.isOrdered() shouldBe true
	}

	test("remove invalid")
	{
		for(x in entries.shuffled(random))
			set.add(x)

		val removeEntries = (1..1000).map({ "n$it" })
		for(x in removeEntries)
			set.remove(x)
		for(x in removeEntries)
			set shouldNotContain x
		for(x in entries)
			set shouldContain x

		set.isOrdered() shouldBe true
	}

	test("remove empty")
	{
		for(x in entries)
			set.remove(x)
		set.iterator().hasNext() shouldBe false
	}

	test("iterator dec")
	{
		for(x in entries)
			set.add(x)

		val iter = set.iterator()
		val nextEntries = mutableListOf<String>()
		while(iter.hasNext())
			nextEntries.add(iter.next())
		val previousEntries = mutableListOf<String>()
		iter.dec()
		while(iter.hasNext())
		{
			previousEntries.add(iter.value())
			iter.dec()
		}

		previousEntries.reversed() shouldBe nextEntries
	}

	test("iterator remove")
	{
		for(x in entries)
			set.add(x)

		val iter = set.iterator()
		var i = 0
		while(iter.hasNext())
		{
			iter.next() shouldBe sortedEntries[i]
			if(i % 2 == 0)
				iter.remove()
			++i
		}

		set.iterator().asSequence().toList() shouldBe
			sortedEntries.filterIndexed({ j, _ -> j % 2 != 0 })

		set.isOrdered() shouldBe true
	}
})
