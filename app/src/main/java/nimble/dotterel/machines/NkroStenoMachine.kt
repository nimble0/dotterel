// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.content.Context
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
	private var strokeOnFirstUp: Boolean = false

	private val keysDown = mutableSetOf<Int>()
	private var lastKeyUp = false

	override var strokeListener: StenoMachine.Listener? = null

	class Factory : StenoMachine.Factory
	{
		override fun availableMachines(context: Context): List<String> = listOf("")
		override fun makeStenoMachine(app: Dotterel, id: String) = NkroStenoMachine(app)
	}

	init
	{
		this.app.addKeyListener(this)
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

			this.strokeOnFirstUp = config.get("strokeOnFirstUp")?.asBoolean() ?: false

			val mapping = systemConfig["NKRO"]!!.asObject()
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
		return if(this.keyMap.isMapped(e.keyCode))
		{
			this.keysDown.add(e.keyCode)

			this.stroke += this.keyMap.parse(e.keyCode)
			this.strokeListener?.changeStroke(this.stroke)

			this.lastKeyUp = true

			true
		}
		else
			false
	}

	override fun keyUp(e: KeyEvent): Boolean
	{
		if(!this.keysDown.remove(e.keyCode))
			return false

		if(this.strokeOnFirstUp)
		{
			if(this.lastKeyUp)
				this.strokeListener?.applyStroke(this.stroke)
			this.stroke = this.keysDown.fold(
				Stroke(this.keyLayout, 0),
				{ acc, it -> acc + this.keyMap.parse(it) })
		}
		else if(this.keysDown.isEmpty())
		{
			this.strokeListener?.applyStroke(this.stroke)
			this.stroke = Stroke(this.keyLayout, 0)
		}

		this.lastKeyUp = false

		return true
	}

	override fun close()
	{
		this.app.removeKeyListener(this)
	}
}
