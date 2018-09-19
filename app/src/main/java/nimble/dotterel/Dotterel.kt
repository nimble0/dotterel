// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast

import java.io.IOException

import nimble.dotterel.machines.*
import nimble.dotterel.translation.*
import nimble.dotterel.translation.systems.IRELAND_LAYOUT
import nimble.dotterel.translation.systems.IRELAND_SYSTEM
import nimble.dotterel.util.BiMap

val KEY_LAYOUTS = BiMap(mapOf(
	Pair("Ireland", IRELAND_LAYOUT)
))

val SYSTEMS = BiMap(mapOf(
	Pair("Ireland", IRELAND_SYSTEM)
))

val CODE_ASSETS = mapOf(
	Pair("dictionaries/Numbers", NumbersDictionary())
)

val MACHINES = mapOf(
	Pair("On Screen", OnScreenStenoMachine.Factory()),
	Pair("NKRO", NkroStenoMachine.Factory())
)

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
		IRELAND_SYSTEM,
		log = { m -> Log.i("Steno", m) })

	val systemName: String get() =
		SYSTEMS.inverted[this.translator.system]
			?: throw IllegalStateException("System missing from systems map")

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

	private fun getDictionary(path: String): Dictionary?
	{
		val error = { message: String ->
			Log.e("Dotterel", message)
			Toast.makeText(
				this,
				message,
				Toast.LENGTH_LONG
			).show()
		}

		try
		{
			val type = path.substringBefore("://")
			val name = path.substringAfter("://")
			return when(type)
			{
				"asset" -> JsonDictionary(this.assets.open(name))
				"code" -> CODE_ASSETS[name] as Dictionary
				else -> JsonDictionary(
					this.contentResolver.openInputStream(Uri.parse(path)))
			}
		}
		catch(e: IOException)
		{
			error("Error reading dictionary $path")
			return null
		}
		catch(e: SecurityException)
		{
			error("Permission denied reading dictionary $path")
			return null
		}
		catch(e: com.eclipsesource.json.ParseException)
		{
			error("$path is not a valid JSON dictionary")
			return null
		}
		catch(e: java.lang.UnsupportedOperationException)
		{
			error("$path is not a valid JSON dictionary")
			return null
		}
		catch(e: ClassCastException)
		{
			error("$path is not of type Dictionary")
			return null
		}
	}

	private fun loadDictionaries()
	{
		val key = "system/${this.systemName}/dictionaries"
		val dictionariesPreference = this.preferences?.getString(key, null)
			?.let({ dictionaryListFromJson(key, it) })

		val dictionaries = dictionariesPreference
			?.filter({ it.enabled })
			?.map({ it.name })
			?: this.translator.system.defaultDictionaries

		this.translator.dictionary = MultiDictionary(
			dictionaries.mapNotNull({ this.getDictionary(it) }))
	}

	private fun addMachine(name: String)
	{
		if(name !in this.machines)
		{
			val machineFactory = MACHINES[name] ?: return
			this.machines[name] = machineFactory
				.makeStenoMachine(this)
				.also({
					it.keyLayout = this.translator.system.keyLayout
					it.strokeListener = this
				})
		}
	}
	private fun removeMachine(name: String)
	{
		val machine = this.machines[name]
		if(machine != null)
		{
			machine.close()
			if(machine is EditorListener)
				this.removeEditorListener(machine)
			if(machine is KeyListener)
				this.removeKeyListener(machine)
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

	private fun onPreferenceChanged(preferences: SharedPreferences, key: String)
	{
		when
		{
			key == "system/${this.systemName}/dictionaries" ->
				this.loadDictionaries()
			key.substring(0, "machine/".length) == "machine/" ->
				this.loadMachines()
		}

		for(m in this.machines)
			m.value.preferenceChanged(preferences, key)
	}
	private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener({
		preferences, key -> this.onPreferenceChanged(preferences, key) })

	override fun onCreate()
	{
		super.onCreate()

		for(resource in PREFERENCE_RESOURCES)
			PreferenceManager.setDefaultValues(this, resource, true)
		this.preferences = android.preference.PreferenceManager
			.getDefaultSharedPreferences(this)
		this.preferences?.registerOnSharedPreferenceChangeListener(
			this.preferenceListener)

		this.loadDictionaries()
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
		val ic = this.currentInputConnection
		this.translator.apply(s)
		for(a in this.translator.flush())
			when(a)
			{
				is FormattedText ->
				{
					ic?.run({
						this.beginBatchEdit()
						// TODO: Make sure that this is deleting the expected content.
						// InputConnection.deleteSurroundingText should not delete
						// half of a surrogate pair or nonexistent characters.
						this.deleteSurroundingText(a.backspaces, 0)
						this.commitText(a.text, 1)
						this.endBatchEdit()
					})
				}
				is KeyCombo -> a.toAndroidKeyEvent()?.run({ ic?.sendKeyEvent(this) })
				is Runnable -> a.run()
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

		return false
	}
	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean
	{
		for(l in this.keyListeners)
			if(l.keyUp(event))
				return true

		return false
	}

	private val keyListeners = mutableListOf<KeyListener>()
	fun addKeyListener(l: KeyListener) = this.keyListeners.add(l).let({ Unit })
	fun removeKeyListener(l: KeyListener) = this.keyListeners.remove(l).let({ Unit })
}
