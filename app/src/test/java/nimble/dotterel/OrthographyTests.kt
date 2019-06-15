// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.RegexOrthography
import nimble.dotterel.translation.RegexWithWordListOrthography
import nimble.dotterel.translation.SimpleOrthography
import nimble.dotterel.translation.apply

class OrthographyTests : FunSpec
({
	test("simple replacements")
	{
		val replacements = SimpleOrthography()

		replacements.add("on", "a", "onna")
		replacements.add("on", "al", "onal")
		replacements.add("tion", "al", "tionall")
		replacements.add("tion", "ale", "tionale")
		replacements.add("nation", "al", "national")
		replacements.add("at", "ive", "attive")
		replacements.add("lub", "i", "lubii")
		replacements.add("lub", "ing", "lubing")
		replacements.add("club", "in", "lubein")
		replacements.add("club", "ing", "clubbing")

		replacements.apply("nation", "al") shouldBe "national"
		replacements.apply("ton", "al") shouldBe "tonal"
		replacements.apply("ton", "ase")shouldBe "tonnase"
		replacements.apply("nation", "ale") shouldBe "nationale"
		replacements.apply("ration", "ala") shouldBe "rationalla"
		replacements.apply("yon", "el") shouldBe null
		replacements.apply("rat", "ivette") shouldBe "rattivette"
		replacements.apply("ratt", "ivette") shouldBe null
		replacements.apply("tian", "aaa") shouldBe null
		replacements.apply("glub", "it") shouldBe "glubiit"
	}

	test("regex replacements")
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

	test("regex with word list replacements")
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
