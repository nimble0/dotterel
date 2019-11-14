// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.*
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast

import androidx.preference.PreferenceManager

import com.eclipsesource.json.Json
import com.eclipsesource.json.ParseException

import java.io.PrintWriter
import java.io.StringWriter

import nimble.dotterel.machines.*
import nimble.dotterel.stenoAppliers.DefaultStenoApplier
import nimble.dotterel.stenoAppliers.RawStenoStenoApplier
import nimble.dotterel.translation.*

val MACHINE_FACTORIES = mapOf<String, () -> StenoMachine.Factory>(
	Pair("On Screen", { OnScreenStenoMachine.Factory() }),
	Pair("NKRO", { NkroStenoMachine.Factory() }),
	Pair("Serial", { SerialStenoMachine.Factory() })
)

fun combineNameId(name: String, id: String) =
	(name.replace("/", "\\/")
		+ if(id != "") ("/" + id.replace("/", "\\/")) else "")

fun combineNameId(nameId: Pair<String, String>) =
	combineNameId(nameId.first, nameId.second)

fun splitNameId(nameId: String) =
	nameId.split(Regex("(?<!\\\\)/"))
		.map({ it.replace("\\/", "/") })
		.let({ Pair(it[0], if(it.size > 1) it[1] else "") })

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

interface StenoApplier
{
	fun isActive(dotterel: Dotterel): Boolean
	fun apply(translator: Translator, stroke: Stroke)
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

class Dotterel : InputMethodService(), StenoMachine.Listener, StenoMachineTracker
{
	interface KeyListener
	{
		fun keyDown(e: KeyEvent): Boolean
		fun keyUp(e: KeyEvent): Boolean
	}

	interface InputStateListener
	{
		fun onStartInput(editorInfo: EditorInfo, restarting: Boolean)
		fun onFinishInput()
		fun onUpdateSelection(old: IntRange, new: IntRange)
	}

	interface IntentListener
	{
		fun onIntent(context: Context, intent: Intent)
	}

	var preferences: SharedPreferences? = null
		private set

	var translator = Translator(
		NULL_SYSTEM,
		dotterel = this,
		log = object : nimble.dotterel.translation.Log
		{
			override fun info(message: String) { Log.i("Dotterel Translation", message) }
			override fun error(message: String) { Log.e("Dotterel Translation", message) }
		})

	private val stenoAppliers = mutableListOf<StenoApplier>(
		RawStenoStenoApplier(),
		DefaultStenoApplier()
	)

	private val machines = mutableMapOf<Pair<String, String>, StenoMachine>()

	private var viewCreated = false
	var view: View? = null
		set(v)
		{
			field = v
			if(this.viewCreated)
			// Not allowed to pass null to setInputView
				this.setInputView(
					this.view
					?: View(this.applicationContext).also({ it.visibility = View.GONE }))
		}
	fun setView(viewId: Int)
	{
		this.view = viewId.let({ this.layoutInflater.inflate(it, null) })
	}

	override val androidContext: Context
		get() = this
	override val intentForwarder by lazy { IntentForwarder(this) }

	private var sleepPreventor: SleepPreventor? = null

	var machineFactories: Map<String, StenoMachine.Factory> = mapOf()

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

	fun configureMachine(nameId: Pair<String, String>)
	{
		try
		{
			val config = Json.parse(
					this.preferences?.getString("machineConfig/${nameId.first}", "{}")
				).asObject()

			this.machines[nameId]?.setConfig(
				this.translator.system.keyLayout,
				config,
				this.translator.system.machineConfig)
		}
		catch(e: ParseException)
		{
			val m = "$nameId machine config has badly formed JSON"
			Log.e("Dotterel", m)
			Toast.makeText(this, m, Toast.LENGTH_LONG).show()
		}
		catch(e: IllegalArgumentException)
		{
			val m = ("Invalid type found while reading $nameId machine"
				+ " config for system ${this.translator.system.path}")
			Log.e("Dotterel", m)
			Toast.makeText(this, m, Toast.LENGTH_LONG).show()
		}
	}

	override fun addMachine(nameId: Pair<String, String>)
	{
		Log.i("Dotterel", "Loading machine $nameId")
		if(nameId in this.machines)
		{
			Log.i("Dotterel", "Machine $nameId already loaded")
			return
		}

		val factory = this.machineFactories[nameId.first]
		if(factory == null)
		{
			Log.i("Dotterel", "Machine factory ${nameId.first} missing")
			return
		}

		this.machines[nameId] = factory
			.makeStenoMachine(this, nameId.second)
			.also({ it.strokeListener = this })
		this.configureMachine(nameId)
	}
	override fun removeMachine(nameId: Pair<String, String>)
	{
		val machine = this.machines[nameId]
		if(machine != null)
		{
			Log.i("Dotterel", "Unloading machine $nameId")
			machine.close()
			this.machines.remove(nameId)
		}
	}
//	fun checkMachine(nameId: Pair<String, String>)
//	{
//		if(this.preferences?.getBoolean("machine/${combineNameId(nameId)}", false) == true)
//			this.addMachine(nameId)
//		else
//			this.removeMachine(nameId)
//	}
//	private fun loadMachineFactories()
//	{
//		for(factory in MACHINE_FACTORIES)
//			factory.value(this)
//	}

	private fun onPreferenceChanged(key: String)
	{
		when
		{
			key == "system" || key == "reloadSystem" ->
				this.loadSystem()
			key.startsWith("machine/") ->
			{
				val nameId = splitNameId(key.substring("machine/".length))
				if(this.preferences?.getBoolean(key, false) == true)
					this.addMachine(nameId)
				else
					this.removeMachine(nameId)
			}
			key.startsWith("machineConfig/") ->
			{
				val nameId = splitNameId(key.substring("machineConfig/".length))
				this.configureMachine(nameId)
			}
		}
	}
	private val preferenceListener =
		SharedPreferences.OnSharedPreferenceChangeListener({ _, key ->
			this.onPreferenceChanged(key) })

	override fun onCreate()
	{
		// Theme isn't correctly set from XML
		this.setTheme(R.style.AppTheme)
		super.onCreate()

		setDefaultSettings(this)

		this.preferences = PreferenceManager
			.getDefaultSharedPreferences(this)
		// Preference listener stored as member variable because
		// SharedPreferences holds listeners with weak pointers.
		this.preferences?.registerOnSharedPreferenceChangeListener(
			this.preferenceListener)

		this.loadSystem()
		this.machineFactories = MACHINE_FACTORIES.mapValues({ it.value() })
		for(f in this.machineFactories)
			f.value.tracker = this
	}

	override fun onDestroy()
	{
		this.intentForwarder.close()
		super.onDestroy()
	}

	override fun onCreateInputView(): View?
	{
		this.viewCreated = true
		(this.view?.parent as? ViewGroup)?.removeView(this.view)
		return this.view
	}

	override fun onEvaluateFullscreenMode(): Boolean
	{
		super.onEvaluateFullscreenMode()
		return false
	}

	override fun changeStroke(s: Stroke) {}
	override fun applyStroke(s: Stroke)
	{
		this.stenoAppliers
			.firstOrNull({ it.isActive(this) })
			?.apply(this.translator, s)

		for(a in this.translator.flush())
			try
			{
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
						a.toAndroidKeyEvents().forEach({
							this.currentInputConnection?.sendKeyEvent(it)
						})
					is Runnable -> a.run()
					is DotterelRunnable -> a(this)
				}
			}
			catch(e: Exception)
			{
				val stackTrace = StringWriter()
					.also({ e.printStackTrace(PrintWriter(it)) })
					.toString()
				Log.e("Dotterel", "Exception executing steno action: $e\n$stackTrace")
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
			}
	}

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

	override fun onStartInput(attribute: EditorInfo, restarting: Boolean)
	{
		super.onStartInput(attribute, restarting)
		for(l in this.inputStateListeners)
			l.onStartInput(attribute, restarting)
	}
	override fun onFinishInput()
	{
		super.onFinishInput()
		for(l in this.inputStateListeners)
			l.onFinishInput()
	}
	override fun onUpdateSelection(
		oldSelStart: Int,
		oldSelEnd: Int,
		newSelStart: Int,
		newSelEnd: Int,
		candidatesStart: Int,
		candidatesEnd: Int)
	{
		super.onUpdateSelection(
			oldSelStart,
			oldSelEnd,
			newSelStart,
			newSelEnd,
			candidatesStart,
			candidatesEnd)

		val old = oldSelStart until oldSelEnd
		val new = newSelStart until newSelEnd
		for(l in this.inputStateListeners)
			l.onUpdateSelection(old, new)
	}
	private val inputStateListeners = mutableListOf<InputStateListener>(ContextSwitcher(this))

	fun checkOverlayPermission(): Boolean =
		Build.VERSION.SDK_INT < Build.VERSION_CODES.M
			|| Settings.canDrawOverlays(this)

	fun requestOverlayPermission()
	{
		if(!this.checkOverlayPermission())
			this.startActivity(
				Intent(
					Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:$packageName")
				).also({ it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); })
			)
	}

	fun poke()
	{
		if(!this.checkOverlayPermission())
			return
		if(this.sleepPreventor == null)
			this.sleepPreventor = SleepPreventor(this)
		this.sleepPreventor?.poke()
	}
}
