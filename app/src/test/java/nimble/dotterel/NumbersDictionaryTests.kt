// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.translation.NumbersDictionary

class NumbersDictionaryTests : FunSpec
({

	test("numbers")
	{
		val layout = KeyLayout(
			"#1S2TK3PW4HR-5A0O*EU-6FR7PB8LG9TSDZ",
			mapOf(
				Pair("1-", listOf("#-", "S-")),
				Pair("2-", listOf("#-", "T-")),
				Pair("3-", listOf("#-", "P-")),
				Pair("4-", listOf("#-", "H-")),
				Pair("5-", listOf("#-", "A-")),
				Pair("0-", listOf("#-", "O-")),
				Pair("-6", listOf("#-", "-F")),
				Pair("-7", listOf("#-", "-P")),
				Pair("-8", listOf("#-", "-L")),
				Pair("-9", listOf("#-", "-T")))
		)
		val dictionary = NumbersDictionary()

		dictionary[layout.parse(listOf("13-"))] shouldBe "{&13}"
		dictionary[layout.parse(listOf("1-9"))] shouldBe "{&19}"
		dictionary[layout.parse(listOf("08"))] shouldBe "{&08}"
		dictionary[layout.parse(listOf("1234506789"))] shouldBe "{&1234506789}"
		dictionary[layout.parse(listOf("K08"))] shouldBe null
		dictionary[layout.parse(listOf("0*8"))] shouldBe null
		dictionary[layout.parse(listOf(""))] shouldBe null
		dictionary[layout.parse(listOf("TPH-"))] shouldBe null
	}
})
