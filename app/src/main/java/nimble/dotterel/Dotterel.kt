// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast

import androidx.preference.PreferenceManager

import java.io.IOException

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
		log = { m -> Log.e("Dotterel Translation", m) })

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
		this.preferences = PreferenceManager
			.getDefaultSharedPreferences(this)
		this.preferences?.registerOnSharedPreferenceChangeListener(
			this.preferenceListener)

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
