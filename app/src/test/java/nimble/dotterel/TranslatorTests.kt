// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import kotlin.math.*

import nimble.dotterel.translation.*
import nimble.dotterel.translation.systems.IRELAND_SYSTEM

internal fun actionsToText(actions: List<Any>): String =
	actions.fold("", { acc, it ->
		if(it is FormattedText)
			acc.substring(0, max(0, acc.length - it.backspaces)) + it.text
		else
			acc
	})

internal fun Translator.apply(strokes: String): List<Any> =
	strokes.split("/").fold(
		listOf(),
		{ acc, it -> acc + this.apply(this.system.keyLayout.parse(it)) })

internal fun Translator.applyToString(strokes: String) =
	actionsToText(this.apply(strokes))

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

	test("bad formatting")
	{
		dictionary["PRE"] = "{^pre^"
		dictionary["HEL"] = "hell"

		translator.applyToString("PRE/HEL") shouldBe " hell"
	}

	test("no space formatting")
	{
		dictionary["PRE"] = "{^pre^}"
		dictionary["HEL"] = "hell"
		dictionary["-D"] = "{^ed}"

		translator.applyToString("PRE/HEL/-D") shouldBe "prehelled"
	}

	test("glue formatting")
	{
		dictionary["HEL"] = "hell"
		dictionary["*E"] = "{&e}"
		dictionary["O*"] = "{&o}"
		dictionary["-D"] = "{^ed}"

		translator.applyToString("HEL/*E/O*/HEL/O*/-D") shouldBe " hell eo hell oed"
	}

	test("multiple translation parts")
	{
		dictionary["HEL"] = "hell"
		dictionary["*E"] = "{^e}eee{&E}{e^}"

		translator.applyToString("HEL/*E/HEL") shouldBe " helle eee E ehell"
	}
})
