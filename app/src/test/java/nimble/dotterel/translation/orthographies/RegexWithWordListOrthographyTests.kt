// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.orthographies

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.apply

class RegexWithWordListOrthographyTests : FunSpec
({
	test("")
	{
		val replacements = RegexWithWordListOrthography(
			listOf(
				RegexWithWordListOrthography.Replacement(
					Regex("(\\S+[bcdfghjklmnpqrstuvwxz])e\uffff([aeiouy]\\w)"),
					"$1$2"),
				RegexWithWordListOrthography.Replacement(
					Regex("(\\S*[bcdfghjklmnprstvwxyz]u)([gbdlmntv])\uffff([aeiouy])"),
					"$1$2$2$3")
			),
			mapOf(Pair("ible", "able")),
			mapOf(
				Pair("narrating", 10),
				Pair("responsible", 10),
				Pair("huter", 20),
				Pair("hutter", 30),
				Pair("hugger", 20)
			)
		)

		replacements.apply("narrate", "ing") shouldBe "narrating"
		replacements.apply("narratt", "ing") shouldBe null
		replacements.apply("bug", "er") shouldBe "bugger"
		replacements.apply("bug", "y") shouldBe "buggy"
		replacements.apply("bug", "ted") shouldBe null
		replacements.apply("response", "ible") shouldBe "responsible"
		replacements.apply("hut", "er") shouldBe "huter"
		replacements.apply("hug", "er") shouldBe "hugger"
	}
})
