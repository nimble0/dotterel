// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.*
import nimble.dotterel.translation.systems.IRELAND_SYSTEM

internal fun Translator.apply(strokes: String): List<Any>
{
	for(s in strokes.split("/"))
		this.apply(this.system.keyLayout.parse(s))
	return this.flush()
}

internal fun Translator.applyToString(strokes: String) =
	actionsToText(this.apply(strokes))?.text ?: ""

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

	test("single transform")
	{
		dictionary["HEL"] = "hell"
		dictionary["-D"] = "{^ed}"
		dictionary["-G"] = "{^ing}"
		dictionary["KPA"] = "{-|}"
		dictionary["KPA*L"] = "{<}"
		dictionary["H*EL"] = "HELL"
		dictionary["HRO*ER"] = "{>}"

		translator.applyToString("KPA/HEL/-D/-G/HEL") shouldBe " Helleding hell"
		translator.applyToString("KPA*L/HEL/-D/-G/HEL") shouldBe " HELLEDING hell"
		translator.applyToString("HRO*ER/H*EL/-D/-G/H*EL") shouldBe " helleding HELL"
	}

	test("carry single transform")
	{
		dictionary["HEL"] = "hell"
		dictionary["-D"] = "{^ed}"
		dictionary["-G"] = "{^ing}"
		dictionary["KPA"] = "{-|}"
		dictionary["KW-BG"] = "{^~|,}"

		translator.applyToString("KPA/KW-BG/HEL") shouldBe ", Hell"
	}

	test("undo")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TRAOEPL"] = "extreme"
		dictionary["EBGS/TAT/EUBG"] = "ecstatic"
		dictionary["*"] = "{RETRO:UNDO}"

		translator.applyToString("EBGS/TAT/EUBG/*/*/TRAOEPL") shouldBe " extreme"
		translator.history.size shouldBe 1
		translator.history.last().replaces.size shouldBe 1
	}

	test("undo past history buffer")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TRAOEPL"] = "extreme"
		dictionary["EBGS/TAT/EUBG"] = "ecstatic"
		dictionary["*"] = "{RETRO:UNDO}"

		val actions = translator.apply("EBGS/TAT/EUBG/*/*/TRAOEPL/*/*/*/*")
		actions.filterIsInstance<KeyCombo>().size shouldBe 2
		actionsToText(actions)?.text ?: "" shouldBe ""
	}

	test("repeat last stroke")
	{
		dictionary["HEL"] = "hell"
		dictionary["PWOPB"] = "bon"
		dictionary["PWOPB/PWOPB"] = "bon-bon"
		dictionary["*"] = "{RETRO:UNDO}"
		dictionary["#*"] = "{RETRO:REPEAT_LAST_STROKE}"

		translator.applyToString("HEL/#*") shouldBe " hell hell"
		translator.applyToString("PWOPB/#*/#*") shouldBe " bon-bon bon"
		translator.applyToString("HEL/PWOPB/#*/#*/*/*/#*") shouldBe " hell bon-bon"
	}

	test("last translation")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TRAOEPL"] = "extreme{<}"
		dictionary["TRAOEPL"] = "treatment"
		dictionary["#*"] = "{RETRO:LAST_TRANSLATION}"
		dictionary["PH-FP"] = "{^~|,} {RETRO:LAST_TRANSLATION}"
		dictionary["PH-FPL"] = "{>} {RETRO:LAST_TRANSLATION} {^~|,} {RETRO:LAST_TRANSLATION}"

		translator.applyToString("EBGS/#*/TRAOEPL") shouldBe " exextreatment"
		translator.applyToString("EBGS/TRAOEPL/#*") shouldBe " extreme EXTREME"
		translator.applyToString("EBGS/TRAOEPL/PH-FP") shouldBe " EXTREME, EXTREME"
		translator.applyToString("EBGS/TRAOEPL/PH-FPL") shouldBe " EXTREME extreme, EXTREME"
	}

	test("last cluster")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TRAOEPL"] = "extreme{<}"
		dictionary["TRAOEPL"] = "treatment"
		dictionary["-L"] = "{^ly}"
		dictionary["#*"] = "{RETRO:LAST_CLUSTER}"
		dictionary["PH-FP"] = "{^~|,} {RETRO:LAST_CLUSTER}"
		dictionary["PH-FPL"] = "{>} {RETRO:LAST_CLUSTER} {^~|,} {RETRO:LAST_CLUSTER}"

		translator.applyToString("EBGS/#*/TRAOEPL") shouldBe " exextreatment"
		translator.applyToString("EBGS/EBGS/TRAOEPL/-L/#*") shouldBe " exextremeLY exextremeLY"
		translator.applyToString("EBGS/EBGS/TRAOEPL/-L/PH-FP") shouldBe " exextremeLY, exextremeLY"
		translator.applyToString("EBGS/EBGS/TRAOEPL/-L/PH-FPL") shouldBe " exextremeLY exextremeLY, exextremeLY"
	}

	test("move last cluster")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TAT/EUBG"] = "ecstatic"
		dictionary["TPAEUS"] = "face"
		dictionary["-G"] = "{^ing}"
		dictionary["PH-FP"] = "{<}{RETRO:MOVE_LAST_CLUSTER}"

		translator.applyToString(
			"EBGS/TPAEUS/PH-FP/EBGS/EBGS/TAT/EUBG/-G/PH-FP/TPAEUS"
		) shouldBe " EXFACE EXECSTATICING face"
	}

	test("retro break translation")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TRAOEPL"] = "extreme"
		dictionary["TRAOEPL"] = "treatment"
		dictionary["TRAOEPL/TOED"] = "trematode"
		dictionary["TPAEUS"] = "face"
		dictionary["TOED"] = "today"
		dictionary["-G"] = "{^ing}"
		dictionary["PH-FP"] = "{RETRO:BREAK_TRANSLATION}"

		translator.applyToString(
			"TPAEUS/EBGS/TRAOEPL/PH-FP"
		) shouldBe " face extreatment"
		translator.applyToString(
			"TPAEUS/EBGS/TRAOEPL/-G/PH-FP"
		) shouldBe " face extreatmenting"
		translator.applyToString(
			"TPAEUS/EBGS/TRAOEPL/PH-FP/TOED"
		) shouldBe " face extreatment today"
	}

	test("toggle asterisk")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TRAOEPL"] = "extreme"
		dictionary["EBGS/TRAO*EPL"] = "extremely"
		dictionary["TRAOEPL"] = "treatment"
		dictionary["TRAOEPL/TOED"] = "trematode"
		dictionary["TOED"] = "today"
		dictionary["TO*ED"] = "toad"
		dictionary["*"] = "{RETRO:UNDO}"
		dictionary["#"] = "{RETRO:LAST_TRANSLATION}"
		dictionary["#*"] = "{RETRO:TOGGLE_ASTERISK}"

		translator.applyToString(
			"EBGS/TRAOEPL/#*"
		) shouldBe " extremely"
		translator.applyToString(
			"TRAOEPL/TO*ED/#*"
		) shouldBe " trematode"
		translator.applyToString(
			"TRAOEPL/TO*ED/#*/*"
		) shouldBe " treatment"
		// Toggle asterisk (#*) combines with last translation (#) to
		// replace the previous translation with another toggle asterisk
		// translation causing "treatment" to become "TRAO*EPL".
		translator.applyToString(
			"TRAOEPL/#/#*"
		) shouldBe " TRAO*EPL"
	}

	test("combination of retro commands")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TRAOEPL"] = "extreme"
		dictionary["TRAOEPL"] = "treatment"
		dictionary["#"] = "{RETRO:LAST_TRANSLATION}"
		dictionary["AFPS"] = "{RETRO:BREAK_TRANSLATION}"
		dictionary["TK-FPS"] = "{^}{RETRO:MOVE_LAST_CLUSTER}"
		dictionary["PH-FP"] = "{<}{RETRO:MOVE_LAST_CLUSTER}"

		translator.applyToString(
			"EBGS/TRAOEPL/#/AFPS/TK-FPS/PH-FP"
		) shouldBe "EXTREATMENT extreme"
	}

	test("optimise text actions")
	{
		val actions = mutableListOf<Any>(
			FormattedText(8, "optimising"),
			FormattedText(14, " deleted"),
			FormattedText(25, " new context"),
			FormattedText(12, " newish context")
		)

		optimiseTextActions(" some context to optimise", actions) shouldBe listOf<Any>(
			FormattedText(1, "ing"),
			FormattedText(13, "deleted"),
			FormattedText(25, " new context"),
			FormattedText(8, "ish context")
		)
	}

	test("remove empty text actions")
	{
		removeEmptyTextActions(listOf(
			FormattedText(0, ""),
			FormattedText(4, ""),
			KeyCombo( "a", 0),
			FormattedText(0, ""),
			FormattedText(0, "hello"),
			KeyCombo( "b", 0),
			FormattedText(0, "")
		)) shouldBe listOf(
			FormattedText(4, ""),
			KeyCombo( "a", 0),
			FormattedText(0, "hello"),
			KeyCombo( "b", 0)
		)
	}

	test("join text actions")
	{
		val formatting = Formatting(transformState = Formatting.TransformState.MAIN)

		joinTextActions(listOf(
			FormattedText(0, "start", formatting),
			FormattedText(4, "ing", formatting),
			KeyCombo( "a", 0),
			FormattedText(3, "middle", formatting),
			FormattedText(1, "hello", formatting),
			KeyCombo( "b", 0),
			FormattedText(0, "end", formatting)
		)) shouldBe listOf(
			FormattedText(0, "sing", formatting),
			KeyCombo( "a", 0),
			FormattedText(3, "middlhello", formatting),
			KeyCombo( "b", 0),
			FormattedText(0, "end", formatting)
		)
	}

	test("optimise actions")
	{
		dictionary["EBGS"] = "{ex^}"
		dictionary["EBGS/TRAOEPL"] = "extreme"
		dictionary["EBGS/TAT/EUBG"] = "ecstatic"
		dictionary["TRAOEPL"] = "treatment"
		dictionary["KPA"] = "{-|}"
		dictionary["KPA*"] = "{-|}{^}"
		dictionary["TK-LS"] = "{^}"
		dictionary["R-R"] = "{#Return}{-|}{^}"
		dictionary["*"] = "{RETRO:UNDO}"

		var actions = translator.apply("KPA/EBGS/TRAOEPL")
		actions.size shouldBe 1

		actions = translator.apply("KPA/TK-LS/KPA*/*/TAT/EUBG")
		actions.size shouldBe 1
		actionsToText(actions)?.text shouldBe "cstatic"

		actions = translator.apply("R-R/*/*/TRAOEPL/KPA")
		actions.size shouldBe 2
		actionsToText(actions)?.text shouldBe "xtreme"
	}
})
