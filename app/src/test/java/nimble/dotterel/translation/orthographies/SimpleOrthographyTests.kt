// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.orthographies

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.apply

class SimpleOrthographyTests : FunSpec
({
	test("")
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

		replacements.apply("Nation", "aL") shouldBe "NationaL"
	}
})
