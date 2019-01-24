// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.*

class StenoStrokeTests : FunSpec
({
	val layout = KeyLayout(
		"#1S2TK3PW4HR",
		"5A0O*EU",
		"6FR7PB8LG9TSDZ",
		mapOf(
			Pair("1-", listOf("#-", "S-")),
			Pair("2-", listOf("#-", "T-")),
			Pair("3-", listOf("#-", "P-")),
			Pair("4-", listOf("#-", "H-")),
			Pair("5-", listOf("#-", "A-")),
			Pair("0-", listOf("#-", "O-")),
			Pair("-6", listOf("#-", "-F")),
			Pair("-7", listOf("#-", "-P")),
			Pair("-8", listOf("#-", "-L")),
			Pair("-9", listOf("#-", "-T")))
	)

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

		// Combination keys
		layout.parse("#STKPWHRAO*EUFRPBLGTSDZ").rtfcre shouldBe "12K3W4R50*EU6R7B8G9SDZ"
		layout.parse("#KWR*EURBGSDZ").rtfcre shouldBe "#KWR*EURBGSDZ"
		layout.parse("#S").rtfcre shouldBe "1"
		layout.parse("#-T").rtfcre shouldBe "-9"
		layout.parse("1").rtfcre shouldBe "1"
		layout.parse("-9").rtfcre shouldBe "-9"
		layout.parse("1T").rtfcre shouldBe "12"
		layout.parse("5T").rtfcre shouldBe "59"

		//Invalid combination key strokes
		layout.parse("8").rtfcre shouldBe ""
		layout.parse("31").rtfcre shouldBe ""
		layout.parse("T1").rtfcre shouldBe ""
	}

	test("parseKeys")
	{
		layout.parseKeys(listOf("H-", "E-", "-L")).rtfcre shouldBe "HEL"
		layout.parseKeys(listOf("H-", "H-")).rtfcre shouldBe "H"
		layout.parseKeys(listOf("R", "P", "T", "-T")).rtfcre shouldBe "TPR-T"
		layout.parseKeys(listOf("A", "O", "*", "E", "U")).rtfcre shouldBe "AO*EU"
	}

	test("pureKeysString")
	{
		layout.parse("STKPWHRAO*EUFRPBLGTSDZ").pureKeysString shouldBe " STKPWHRAO*EUFRPBLGTSDZ"
		layout.parse("STKPWHRAO-EUFRPBLGTSDZ").pureKeysString shouldBe " STKPWHRAO EUFRPBLGTSDZ"
		layout.parse("R").pureKeysString shouldBe "       R               "
		layout.parse("-R").pureKeysString shouldBe "              R        "
		layout.parse("RE").pureKeysString shouldBe "       R   E           "
		layout.parse("ER").pureKeysString shouldBe "           E  R        "

		// Invalid strokes
		layout.parse("TT").pureKeysString shouldBe "                       "
		layout.parse("G").pureKeysString shouldBe "                       "
		layout.parse("TS").pureKeysString shouldBe "                       "

		// Combination keys
		layout.parse("#STKPWHRAO*EUFRPBLGTSDZ").pureKeysString shouldBe "#STKPWHRAO*EUFRPBLGTSDZ"
		layout.parse("#KWR*EURBGSDZ").pureKeysString shouldBe "#  K W R  *EU R B G SDZ"
		layout.parse("#S").pureKeysString shouldBe "#S                     "
		layout.parse("#-T").pureKeysString shouldBe "#                  T   "
		layout.parse("1").pureKeysString shouldBe "#S                     "
		layout.parse("-9").pureKeysString shouldBe "#                  T   "
	}

	test("plus/minus")
	{
		layout.parse("S") + layout.parse("P") shouldBe layout.parse("SP")
		layout.parse("S") + layout.parse("E") shouldBe layout.parse("SE")
		layout.parse("S-P") + layout.parse("-P") shouldBe layout.parse("S-P")
		layout.parse("S") + layout.parse("#") shouldBe layout.parse("1")
		layout.parse("S-P") + layout.parse("#") shouldBe layout.parse("1-7")

		layout.parse("TPR") - layout.parse("TP") shouldBe layout.parse("R")
		layout.parse("TPR") - layout.parse("TP-R") shouldBe layout.parse("R")
		layout.parse("12") - layout.parse("T") shouldBe layout.parse("1")
		layout.parse("12") - layout.parse("ST") shouldBe layout.parse("#")
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
