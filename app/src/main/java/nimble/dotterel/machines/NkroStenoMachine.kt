// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.content.SharedPreferences
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast

import nimble.dotterel.*
import nimble.dotterel.translation.*

class NkroStenoMachine(private val app: Dotterel) :
	StenoMachine, Dotterel.KeyListener
{
	override var keyLayout: KeyLayout = EMPTY_KEY_LAYOUT
		set(v)
		{
			field = v
			this.updateKeyMap()
			this.stroke = Stroke(this.keyLayout, 0)
			this.keysDown.clear()
		}
	private var keyMap: Map<Int, Stroke> = mapOf()
	private var stroke: Stroke = Stroke(this.keyLayout, 0)

	private val keysDown = mutableSetOf<Int>()

	override var strokeListener: StenoMachine.Listener? = null

	class Factory : StenoMachine.Factory
	{
		override fun makeStenoMachine(app: Dotterel) =
			NkroStenoMachine(app).also({ app.addKeyListener(it) })
	}

	private val keyMapPreferenceKey
		get() = "machine/NKRO/key_map/${KEY_LAYOUTS.inverted[this.keyLayout]}"

	private fun updateKeyMap()
	{
		val keyMapPreference = this.app.preferences
			?.getString(keyMapPreferenceKey, null)
			?.let({ keyMapFromJson(keyMapPreferenceKey, it) })
			?: mapOf()

		if(keyMapPreference.isEmpty())
		{
			val m = "Missing/invalid NKRO key mapping for the current key layout"
			Log.e("Dotterel", m)
			Toast.makeText(this.app, m, Toast.LENGTH_SHORT).show()
		}

		// Keymap is stored as a map of stenoKey to list of keyboard keys,
		// but in this class we use a map of keyboard key to stenoKey.
		val keyMap = mutableMapOf<Int, Stroke>()
		for(mapping in keyMapPreference)
		{
			val stenoKey = mapping.key
			for(key in mapping.value)
				keyMap[stringToKeyCode(key)] = this.keyLayout.parse(stenoKey)
		}
		this.keyMap = keyMap
	}

	override fun preferenceChanged(preferences: SharedPreferences, key: String)
	{
		when(key)
		{
			keyMapPreferenceKey -> this.updateKeyMap()
		}
	}

	override fun keyDown(e: KeyEvent): Boolean
	{
		val keyStroke = this.keyMap[e.keyCode] ?: return false

		if(this.stroke.keys or keyStroke.keys != this.stroke.keys)
		{
			this.stroke += keyStroke
			this.strokeListener?.changeStroke(this.stroke)
		}
		this.keysDown.add(e.keyCode)

		return true
	}

	override fun keyUp(e: KeyEvent): Boolean
	{
		if(e.keyCode !in this.keysDown)
			return false

		this.keysDown.remove(e.keyCode)
		if(this.keysDown.isEmpty())
		{
			this.strokeListener?.applyStroke(this.stroke)
			this.stroke = Stroke(this.keyLayout, 0)
		}

		return true
	}

	override fun close() {}
}
