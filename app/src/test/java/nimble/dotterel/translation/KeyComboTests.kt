// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.FunSpec

import java.text.ParseException

class KeyComboTests : FunSpec
({
	test("no modifiers")
	{
		parseKeyCombos("a") shouldBe listOf(KeyCombo("a", 0))
		parseKeyCombos("a b c") shouldBe listOf(
			KeyCombo("a", 0),
			KeyCombo("b", 0),
			KeyCombo("c", 0)
		)
		parseKeyCombos("   a		 b \n c") shouldBe listOf(
			KeyCombo("a", 0),
			KeyCombo("b", 0),
			KeyCombo("c", 0)
		)
	}

	test("modifiers")
	{
		parseKeyCombos("control(a)") shouldBe listOf(
			KeyCombo("a", Modifier.CONTROL.mask)
		)
		parseKeyCombos("control(a alt(b) c) d") shouldBe listOf(
			KeyCombo("a", Modifier.CONTROL.mask),
			KeyCombo("b", Modifier.CONTROL.mask or Modifier.ALT.mask),
			KeyCombo("c", Modifier.CONTROL.mask),
			KeyCombo("d", 0)
		)
		parseKeyCombos("   control	 (a alt\n(b  ) c\n) d 	") shouldBe listOf(
			KeyCombo("a", Modifier.CONTROL.mask),
			KeyCombo("b", Modifier.CONTROL.mask or Modifier.ALT.mask),
			KeyCombo("c", Modifier.CONTROL.mask),
			KeyCombo("d", 0)
		)
	}

	test("invalid")
	{
		// Modifier already set
		shouldThrow<ParseException> { parseKeyCombos("control(control(a))") }
		// Missing open parentheses
		shouldThrow<ParseException> { parseKeyCombos("control(a))") }
		// Invalid modifier
		shouldThrow<ParseException> { parseKeyCombos("notamodifier(a)") }
		// Missing close parentheses
		shouldThrow<ParseException> { parseKeyCombos("control(a") }
		// Invalid characters
		shouldThrow<ParseException> { parseKeyCombos("$(a)") }
		shouldThrow<ParseException> { parseKeyCombos("control(a ')") }
	}
})
