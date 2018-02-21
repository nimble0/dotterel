// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.*

class TranslatorTests : FunSpec
({
	val layout = KeyLayout("STKPWHR", "AO*EU", "FRPBLGTSDZ")
	val translator = Translator()
	val dictionary = StandardDictionary()
	translator.dictionary = dictionary

	test("basic stroke translation")
	{
		dictionary["HEL"] = "hell"
		dictionary["HEL/HROE"] = "hello"
		dictionary["HEL/HROE/THR"] = "hello there!"
		dictionary["THR/PWEU"] = "thereby"

		var translation = translator.translate(layout.parse("HEL"))
		translation.raw shouldBe "hell"
		translation.replaces.size shouldBe 0
		translator.apply(layout.parse("HEL"))

		translation = translator.translate(layout.parse("HROE"))
		translation.raw shouldBe "hello"
		translation.replaces.size shouldBe 1
		translator.apply(layout.parse("HROE"))

		translation = translator.translate(layout.parse("THR"))
		translation.raw shouldBe "hello there!"
		translation.replaces.size shouldBe 1
		translator.apply(layout.parse("THR"))

		translation = translator.translate(layout.parse("PWEU"))
		translation.raw shouldBe "PWEU"
		translation.replaces.size shouldBe 0
	}
})
