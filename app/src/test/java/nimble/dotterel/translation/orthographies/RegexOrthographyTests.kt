// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.orthographies

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.apply

class RegexOrthographyTests : FunSpec
({
	test("")
	{
		val replacements = RegexOrthography(listOf(
			RegexOrthography.Replacement(
				Regex("(\\w[bcdfghjklmnpqrstuvwxz])e\uffff([aeiouy]\\w)"),
				"$1$2"),
			RegexOrthography.Replacement(
				Regex("([bcdfghjklmnprstvwxyz]u)([gbdlmntv])\uffff([aeiouy])"),
				"$1$2$2$3")
		))

		replacements.apply("narrate", "ing") shouldBe "narrating"
		replacements.apply("narratt", "ing") shouldBe null
		replacements.apply("bug", "er") shouldBe "bugger"
		replacements.apply("bug", "y") shouldBe "buggy"
		replacements.apply("bug", "ted") shouldBe null
	}
})
