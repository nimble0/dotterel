// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.*

class StenoStrokeTests : FunSpec
({
	val layout = KeyLayout("STKPWHR", "AO*EU", "FRPBLGTSDZ")

	test("rtfcre")
	{
		layout.parse("STKPWHRAO*EUFRPBLGTSDZ").rtfcre shouldBe "STKPWHRAO*EUFRPBLGTSDZ"
		layout.parse("STKPWHRAO-EUFRPBLGTSDZ").rtfcre shouldBe "STKPWHRAOEUFRPBLGTSDZ"
		layout.parse("R").rtfcre shouldBe "R"
		layout.parse("-R").rtfcre shouldBe "-R"
		layout.parse("RE").rtfcre shouldBe "RE"
		layout.parse("ER").rtfcre shouldBe "ER"

		// Invalid strokes
		layout.parse("TT").rtfcre shouldBe ""
		layout.parse("G").rtfcre shouldBe ""
		layout.parse("TS").rtfcre shouldBe ""
	}

	test("parseKeys")
	{
		layout.parseKeys(listOf("H-", "E-", "-L")).rtfcre shouldBe "HEL"
		layout.parseKeys(listOf("H-", "H-")).rtfcre shouldBe "H"
		layout.parseKeys(listOf("R", "P", "T", "-T")).rtfcre shouldBe "TPR-T"
		layout.parseKeys(listOf("A", "O", "*", "E", "U")).rtfcre shouldBe "AO*EU"
	}

	test("keyString")
	{
		layout.parse("STKPWHRAO*EUFRPBLGTSDZ").keyString shouldBe "STKPWHRAO*EUFRPBLGTSDZ"
		layout.parse("STKPWHRAO-EUFRPBLGTSDZ").keyString shouldBe "STKPWHRAO EUFRPBLGTSDZ"
		layout.parse("R").keyString shouldBe "      R               "
		layout.parse("-R").keyString shouldBe "             R        "
		layout.parse("RE").keyString shouldBe "      R   E           "
		layout.parse("ER").keyString shouldBe "          E  R        "

		// Invalid strokes
		layout.parse("TT").keyString shouldBe "                      "
		layout.parse("G").keyString shouldBe "                      "
		layout.parse("TS").keyString shouldBe "                      "
	}

	test("plus/minus")
	{
		layout.parse("S") + layout.parse("P") shouldBe layout.parse("SP")
		layout.parse("S") + layout.parse("E") shouldBe layout.parse("SE")
		layout.parse("S-P") + layout.parse("-P") shouldBe layout.parse("S-P")

		layout.parse("TPR") - layout.parse("TP") shouldBe layout.parse("R")
		layout.parse("TPR") - layout.parse("TP-R") shouldBe layout.parse("R")
	}

	test("List<Stroke>.rtfcre")
	{
		listOf(
			layout.parse("EBGS"),
			layout.parse("TAT"),
			layout.parse("EUBG")
		).rtfcre shouldBe "EBGS/TAT/EUBG"
	}
})
