// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

class StandardDictionaryTests : FunSpec
({
	val dictionary = StandardDictionary()

	test("longestKey")
	{
		dictionary.longestKey shouldBe 0

		dictionary["HEL"] = "hell"
		dictionary.longestKey shouldBe 1

		dictionary["EBGS/TAT/EUBG"] = "ecstatic"
		dictionary.longestKey shouldBe 3

		dictionary["HEL/LOE"] = "hello"
		dictionary["TPHO/W-R"] = "nowhere"
		dictionary.longestKey shouldBe 3

		dictionary.remove("EBGS/TAT/EUBG")
		dictionary.longestKey shouldBe 2

		dictionary.remove("HEL")
		dictionary.longestKey shouldBe 2

		dictionary.remove("TPHO/W-R")
		dictionary.longestKey shouldBe 2

		dictionary["HEL/LOE"] = "hello"
		dictionary.remove("HEL/LOE")
		dictionary.longestKey shouldBe 0
	}

	test("get")
	{
		dictionary["HEL"] = "hell"
		dictionary["HEL/LOE"] = "hello"
		dictionary["HEL"] shouldBe "hell"
		dictionary["HEL/LOE"] shouldBe "hello"

		dictionary["HEL/LOE"] = "hell low"
		dictionary["HEL/LOE"] shouldBe "hell low"
	}
})
