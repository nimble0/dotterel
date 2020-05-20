// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.translation.Stroke
import nimble.dotterel.util.CaseInsensitiveString

class BackedDictionaryTests : FunSpec
({
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
	val dictionary = BackedDictionary(layout, BackingDictionary())

	test("longestKey")
	{
		dictionary.longestKey shouldBe 0

		dictionary["HEL"] = "hell"
		dictionary.longestKey shouldBe 1

		dictionary["EBGS/TAT/EUBG"] = "ecstatic"
		dictionary.longestKey shouldBe 3

		dictionary["HEL/HROE"] = "hello"
		dictionary["TPHO/W-R"] = "nowhere"
		dictionary.longestKey shouldBe 3

		dictionary.remove("EBGS/TAT/EUBG")
		dictionary.longestKey shouldBe 2

		dictionary.remove("HEL")
		dictionary.longestKey shouldBe 2

		dictionary.remove("TPHO/W-R")
		dictionary.longestKey shouldBe 2

		dictionary["HEL/HROE"] = "hello"
		dictionary.remove("HEL/HROE")
		dictionary.longestKey shouldBe 0
	}

	test("get")
	{
		dictionary["HEL"] = "hell"
		dictionary["HEL/HROE"] = "hello"
		dictionary["HEL"] shouldBe "hell"
		dictionary["HEL/HROE"] shouldBe "hello"

		dictionary["HEL/HROE"] = "hell low"
		dictionary["HEL/HROE"] shouldBe "hell low"
	}

	test("reverseGet")
	{
		dictionary["HEL"] = "hell"
		dictionary["HEL/HROE"] = "hello"
		dictionary["H-L"] = "hello"
		dictionary["KHRARBG"] = "clark"
		dictionary["KHRA*RBG"] = "Clark"
		dictionary["KPRAOEPL"] = "extreme"

		dictionary.reverseGet("hell") shouldBe setOf("HEL")
			.map({ layout.parse(it.split('/')) }).toSet()
		dictionary.reverseGet("hello") shouldBe setOf("HEL/HROE", "H-L")
			.map({ layout.parse(it.split('/')) }).toSet()
		dictionary.reverseGet("clark") shouldBe setOf("KHRARBG")
			.map({ layout.parse(it.split('/')) }).toSet()
		dictionary.reverseGet("Clark") shouldBe setOf("KHRA*RBG")
			.map({ layout.parse(it.split('/')) }).toSet()
		dictionary.reverseGet("extreme") shouldBe setOf("KPRAOEPL")
			.map({ layout.parse(it.split('/')) }).toSet()
		dictionary.reverseGet("missing") shouldBe setOf<List<Stroke>>()

		dictionary.findTranslations(CaseInsensitiveString("hell")) shouldBe
			setOf("hell")
		dictionary.findTranslations(CaseInsensitiveString("hello")) shouldBe
			setOf("hello")
		dictionary.findTranslations(CaseInsensitiveString("clark")) shouldBe
			setOf("clark", "Clark")
		dictionary.findTranslations(CaseInsensitiveString("Clark")) shouldBe
			setOf("clark", "Clark")
		dictionary.findTranslations(CaseInsensitiveString("extreme")) shouldBe
			setOf("extreme")
		dictionary.findTranslations(CaseInsensitiveString("missing")) shouldBe
			setOf<String>()

		dictionary.remove("HEL/HROE")
		dictionary.remove("KHRARBG")

		dictionary.reverseGet("hello") shouldBe setOf("H-L")
			.map({ layout.parse(it.split('/')) }).toSet()
		dictionary.reverseGet("clark") shouldBe setOf<List<Stroke>>()
		dictionary.reverseGet("Clark") shouldBe setOf("KHRA*RBG")
			.map({ layout.parse(it.split('/')) }).toSet()

		dictionary.findTranslations(CaseInsensitiveString("hello")) shouldBe
			setOf("hello")
		dictionary.findTranslations(CaseInsensitiveString("clark")) shouldBe
			setOf("Clark")
		dictionary.findTranslations(CaseInsensitiveString("Clark")) shouldBe
			setOf("Clark")
	}
})
