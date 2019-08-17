// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import com.eclipsesource.json.JsonObject

import java.io.Closeable

import nimble.dotterel.translation.Stroke
import nimble.dotterel.translation.KeyLayout

interface StenoMachine : Closeable
{
	interface Factory
	{
		fun makeStenoMachine(app: Dotterel): StenoMachine
	}

	interface Listener
	{
		fun changeStroke(s: Stroke)
		fun applyStroke(s: Stroke)
	}

	fun setConfig(
		keyLayout: KeyLayout,
		config: JsonObject,
		systemConfig: JsonObject)

	// The key layout of strokes passed to strokeListener must match keyLayout
	var strokeListener: Listener?
}
