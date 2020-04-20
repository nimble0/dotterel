// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.stenoAppliers

import nimble.dotterel.Dotterel
import nimble.dotterel.StenoApplier
import nimble.dotterel.translation.Stroke
import nimble.dotterel.translation.Translator
import nimble.dotterel.translation.applyRaw

class RawStenoStenoApplier : StenoApplier
{
	override fun isActive(dotterel: Dotterel): Boolean
	{
		val privateImeOptions = (dotterel.currentInputEditorInfo.privateImeOptions ?: "")
			.split(",")
			.map({
				val parts = it.split("=")
				Pair(parts[0], parts.getOrNull(1))
			})
			.toMap()

		return privateImeOptions["nimble.dotterel.rawSteno"] == "true"
	}

	override fun apply(translator: Translator, stroke: Stroke) =
		translator.applyRaw(stroke)
}
