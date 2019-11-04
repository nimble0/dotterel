// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context

interface StenoMachineTracker
{
	val androidContext: Context
	val intentForwarder: IntentForwarder

	fun addMachine(nameId: Pair<String, String>)
	fun removeMachine(nameId: Pair<String, String>)
}
