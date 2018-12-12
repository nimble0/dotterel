// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.view.KeyEvent

import com.eclipsesource.json.JsonObject

import nimble.dotterel.*
import nimble.dotterel.translation.*
import nimble.dotterel.util.mapValues

class NkroStenoMachine(private val app: Dotterel) :
	StenoMachine, Dotterel.KeyListener
{
	private var keyLayout: KeyLayout = KeyLayout("")
	private var keyMap: KeyMap<Int> = KeyMap(KeyLayout(""), mapOf(), { 0 })
	private var stroke: Stroke = Stroke(this.keyLayout, 0)

	private val keysDown = mutableSetOf<Int>()

	override var strokeListener: StenoMachine.Listener? = null

	class Factory : StenoMachine.Factory
	{
		override fun makeStenoMachine(app: Dotterel) =
			NkroStenoMachine(app).also({ app.addKeyListener(it) })
	}

	override fun setConfig(
		keyLayout: KeyLayout,
		config: JsonObject,
		systemConfig: JsonObject)
	{
		try
		{
			this.keyLayout = keyLayout
			this.stroke = Stroke(this.keyLayout, 0)

			val mapping = systemConfig
				.get("layout").asObject()
				.mapValues({ it.value.asArray().map({ key -> key.asString() }) })

			this.keyMap = KeyMap(this.keyLayout, mapping, { stringToKeyCode(it) })

			this.keysDown.clear()
		}
		catch(e: java.lang.NullPointerException)
		{
			throw IllegalArgumentException(e)
		}
		catch(e: java.lang.UnsupportedOperationException)
		{
			throw IllegalArgumentException(e)
		}
	}

	override fun keyDown(e: KeyEvent): Boolean
	{
		val keyStroke = this.keyMap.parse(e.keyCode)
		if(keyStroke.isEmpty)
			return false

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
