// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast

import androidx.preference.PreferenceManager

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.ParseException

import nimble.dotterel.machines.*
import nimble.dotterel.translation.*

val MACHINES = mapOf(
	Pair("On Screen", OnScreenStenoMachine.Factory()),
	Pair("NKRO", NkroStenoMachine.Factory())
)

interface DotterelRunnable
{
	operator fun invoke(dotterel: Dotterel)

	companion object
	{
		fun make(lambda: (Dotterel) -> Unit) =
			object : DotterelRunnable
			{
				override fun invoke(dotterel: Dotterel) = lambda(dotterel)
			}
	}
}

fun reloadSystem(sharedPreferences: SharedPreferences, system: String)
{
	if(system != sharedPreferences.getString("system", null))
		return

	val current = sharedPreferences.getBoolean("reloadSystem", false)
	sharedPreferences
		.edit()
		.putBoolean("reloadSystem", !current)
		.apply()
}

class Dotterel : InputMethodService(), StenoMachine.Listener
{
	interface EditorListener
	{
		fun setInputView(v: View?)
	}

	interface KeyListener
	{
		fun keyDown(e: KeyEvent): Boolean
		fun keyUp(e: KeyEvent): Boolean
	}

	var preferences: SharedPreferences? = null
		private set

	var translator = Translator(
		NULL_SYSTEM,
		log = object : nimble.dotterel.translation.Log
		{
			override fun info(message: String) { Log.i("Dotterel Translation", message) }
			override fun error(message: String) { Log.e("Dotterel Translation", message) }
		})

	private val machines = mutableMapOf<String, StenoMachine>()

	private var viewCreated = false
	var viewId: Int? = null
		set(v)
		{
			field = v
			if(this.viewCreated)
			{
				val view = v?.let({ this.layoutInflater.inflate(it, null) })
				for(l in this.editorListeners)
					l.setInputView(view)

				// Not allowed to pass null to setInputView
				this.setInputView(view ?: View(this.applicationContext))
			}
		}

	private fun loadSystem()
	{
		val system = this.preferences?.getString("system", null)
		this.loadSystem(system)
	}

	fun setSystem(system: String?)
	{
		this.preferences?.edit()?.putString("system", system)?.apply()
		this.loadSystem(system)
	}

	private fun loadSystem(name: String?)
	{
		Log.i("Dotterel", "Load system $name")

		val system = if(name == null)
			NULL_SYSTEM
		else
			(this.application as DotterelApplication)
				.systemManager
				.openSystem(name)
				?: return

		this.translator.system = system
		for(machine in this.machines.keys)
			this.configureMachine(machine)
	}

	private fun configureMachine(machine: String)
	{
		try
		{
			val config = Json.parse(
					this.preferences?.getString("machineConfig/$machine", "{}")
				).asObject()
			val systemConfig = this.translator.system.machineConfig[machine]
				?.asObject()
				?: JsonObject()

			this.machines[machine]?.setConfig(
				this.translator.system.keyLayout,
				config,
				systemConfig)
		}
		catch(e: ParseException)
		{
			val m = "$machine machine config has badly formed JSON"
			Log.e("Dotterel", m)
			Toast.makeText(this, m, Toast.LENGTH_LONG).show()
		}
		catch(e: IllegalArgumentException)
		{
			val m = ("Invalid type found while reading $machine machine"
				+ " config for system ${this.translator.system.path}")
			Log.e("Dotterel", m)
			Toast.makeText(this, m, Toast.LENGTH_LONG).show()
		}
	}

	private fun addMachine(name: String)
	{
		if(name !in this.machines)
		{
			val machineFactory = MACHINES[name] ?: return
			this.machines[name] = machineFactory
				.makeStenoMachine(this)
				.also({ it.strokeListener = this })
			this.configureMachine(name)
		}
	}
	private fun removeMachine(name: String)
	{
		val machine = this.machines[name]
		if(machine != null)
		{
			machine.close()
			this.machines.remove(name)
		}
	}
	private fun loadMachines()
	{
		for(m in MACHINES)
		{
			if(this.preferences?.getBoolean("machine/${m.key}", false) == true)
				this.addMachine(m.key)
			else
				this.removeMachine(m.key)
		}
	}

	private fun onPreferenceChanged(key: String)
	{
		when
		{
			key == "system" || key == "reloadSystem" ->
				this.loadSystem()
			key.startsWith("machine/") ->
			{
				val machine = key.substring("machine/".length)
				if(this.preferences?.getBoolean(key, false) == true)
					this.addMachine(machine)
				else
					this.removeMachine(machine)
			}
			key.startsWith("machineConfig/") ->
			{
				val machine = key.substring("machineConfig/".length)
				this.configureMachine(machine)
			}
		}
	}
	private val preferenceListener =
		SharedPreferences.OnSharedPreferenceChangeListener({ _, key ->
			this.onPreferenceChanged(key) })

	override fun onCreate()
	{
		super.onCreate()

		setDefaultSettings(this)

		this.preferences = PreferenceManager
			.getDefaultSharedPreferences(this)
		// Preference listener stored as member variable because
		// SharedPreferences holds listeners with weak pointers.
		this.preferences?.registerOnSharedPreferenceChangeListener(
			this.preferenceListener)

		this.loadSystem()
		this.loadMachines()
	}

	override fun onCreateInputView(): View?
	{
		this.viewCreated = true
		val view = this.viewId?.let({ this.layoutInflater.inflate(it, null) })
		for(l in this.editorListeners)
			l.setInputView(view)
		return view
	}

	override fun onEvaluateFullscreenMode(): Boolean
	{
		super.onEvaluateFullscreenMode()
		return false
	}

	override fun changeStroke(s: Stroke) {}
	override fun applyStroke(s: Stroke)
	{
		this.translator.apply(s)
		for(a in this.translator.flush())
			when(a)
			{
				is FormattedText ->
				{
					this.currentInputConnection?.run({
						this.beginBatchEdit()
						// TODO: Make sure that this is deleting the expected content.
						// InputConnection.deleteSurroundingText should not delete
						// half of a surrogate pair or nonexistent characters.
						this.deleteSurroundingText(a.backspaces, 0)
						this.commitText(a.text, 1)
						this.endBatchEdit()
					})
				}
				is KeyCombo ->
					a.toAndroidKeyEvent()?.also({
						this.currentInputConnection?.sendKeyEvent(it)
					})
				is Runnable -> a.run()
				is DotterelRunnable -> a(this)
			}
	}

	private val editorListeners = mutableListOf<EditorListener>()
	fun addEditorListener(l: EditorListener) = this.editorListeners.add(l).let({ Unit })
	fun removeEditorListener(l: EditorListener) = this.editorListeners.remove(l).let({ Unit })

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
	{
		for(l in this.keyListeners)
			if(l.keyDown(event))
				return true

		return super.onKeyDown(keyCode, event)
	}
	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean
	{
		for(l in this.keyListeners)
			if(l.keyUp(event))
				return true

		return super.onKeyUp(keyCode, event)
	}

	private val keyListeners = mutableListOf<KeyListener>()
	fun addKeyListener(l: KeyListener) = this.keyListeners.add(l).let({ Unit })
	fun removeKeyListener(l: KeyListener) = this.keyListeners.remove(l).let({ Unit })
}
