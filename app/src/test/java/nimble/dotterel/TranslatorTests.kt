// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.*
import nimble.dotterel.translation.systems.IRELAND_SYSTEM

class TranslatorTests : FunSpec
({
	val system = IRELAND_SYSTEM
	val layout = system.keyLayout
	val translator = Translator(system)
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
		translation.fullMatch shouldBe true
		translator.apply(layout.parse("HEL"))

		translation = translator.translate(layout.parse("HROE"))
		translation.raw shouldBe "hello"
		translation.replaces.size shouldBe 1
		translation.fullMatch shouldBe true
		translator.apply(layout.parse("HROE"))

		translation = translator.translate(layout.parse("THR"))
		translation.raw shouldBe "hello there!"
		translation.replaces.size shouldBe 1
		translator.apply(layout.parse("THR"))

		translation = translator.translate(layout.parse("PWEU"))
		translation.raw shouldBe "PWEU"
		translation.replaces.size shouldBe 0
		translation.fullMatch shouldBe false
	}

	test("suffix stroke translation")
	{
		dictionary["HEL"] = "hell"
		dictionary["HEL/HROE"] = "hello"
		dictionary["HROED"] = "lode"
		dictionary["-D"] = "{^ed}"

		var translation = translator.translate(layout.parse("HELD"))
		translation.raw shouldBe " hell {^ed}"
		translation.replaces.size shouldBe 0
		translation.fullMatch shouldBe false

		translator.apply(layout.parse("HEL"))
		translation = translator.translate(layout.parse("HROED"))
		translation.raw shouldBe "lode"
		translation.replaces.size shouldBe 0
		translation.fullMatch shouldBe true
	}

	test("prefix + suffix stroke translation")
	{
		val testSystem = IRELAND_SYSTEM.copy(
			prefixStrokes = listOf("#")
		)

		@Suppress("NAME_SHADOWING")
		val translator = Translator(testSystem)
		translator.dictionary = dictionary

		dictionary["HEL"] = "hell"
		dictionary["HEL/HROE"] = "hello"
		dictionary["HROED"] = "lode"
		dictionary["HEL/4E8"] = "hellolleh"
		dictionary["#"] = "the"
		dictionary["-D"] = "{^ed}"
		dictionary["-G"] = "{^ing}"

		var translation = translator.translate(layout.parse("HELD"))
		translation.raw shouldBe " hell {^ed}"
		translation.replaces.size shouldBe 0
		translation.fullMatch shouldBe false

		translation = translator.translate(layout.parse("4E8"))
		translation.raw shouldBe "the hell "
		translation.replaces.size shouldBe 0
		translation.fullMatch shouldBe false

		translator.apply(layout.parse("4E8"))
		translation = translator.translate(layout.parse("HROEG"))
		translation.raw shouldBe "the hello {^ing}"
		translation.replaces.size shouldBe 1
		translation.fullMatch shouldBe false

		translator.apply(layout.parse("4E8"))
		translation = translator.translate(layout.parse("HROED"))
		translation.raw shouldBe "lode"
		translation.replaces.size shouldBe 0
		translation.fullMatch shouldBe true

		translator.apply(layout.parse("HEL"))
		translation = translator.translate(layout.parse("4E8"))
		translation.raw shouldBe "hellolleh"
		translation.replaces.size shouldBe 1
		translation.fullMatch shouldBe true
	}
})
