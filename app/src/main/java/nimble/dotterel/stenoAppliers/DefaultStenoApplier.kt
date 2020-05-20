// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.stenoAppliers

import nimble.dotterel.Dotterel
import nimble.dotterel.StenoApplier
import nimble.dotterel.translation.Stroke
import nimble.dotterel.translation.Translator

class DefaultStenoApplier : StenoApplier
{
	override fun isActive(dotterel: Dotterel): Boolean = true

	override fun apply(translator: Translator, stroke: Stroke) =
		translator.apply(stroke)
}
