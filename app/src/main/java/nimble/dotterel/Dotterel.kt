// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.util.Log
import android.view.View

import java.io.IOException

import nimble.dotterel.machines.TouchStenoView
import nimble.dotterel.translation.*
import nimble.dotterel.translation.systems.IRELAND_SYSTEM
import nimble.dotterel.util.BiMap

val SYSTEMS = BiMap(mapOf(
	Pair("Ireland", IRELAND_SYSTEM)
))

val CODE_ASSETS = mapOf(
	Pair("dictionaries/Numbers", NumbersDictionary())
)

class Dotterel : InputMethodService(), StrokeListener
{
	var preferences: SharedPreferences? = null
		private set

	private var touchSteno: TouchStenoView? = null
	private var translator = Translator(
		IRELAND_SYSTEM,
		log = { m -> Log.i("Steno", m) })

	val systemName: String get() =
		SYSTEMS.inverted[this.translator.system]
			?: throw IllegalStateException("System missing from systems map")

	private fun getDictionary(path: String): Dictionary?
	{
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
			Log.i("IO", "Error reading dictionary $path")
			return null
		}
		catch(e: java.lang.IllegalStateException)
		{
			Log.i("Dictionary", "$path is not a valid JSON dictionary")
			return null
		}
		catch(e: ClassCastException)
		{
			Log.i("Type", "$path is not of type Dictionary")
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

	private fun onPreferenceChanged(preferences: SharedPreferences, key: String)
	{
		if(key == "system/${this.systemName}/dictionaries")
			this.loadDictionaries()
	}
	private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener({
		preferences, key -> this.onPreferenceChanged(preferences, key) })

	override fun onCreate()
	{
		super.onCreate()

		this.preferences = android.preference.PreferenceManager
			.getDefaultSharedPreferences(this)
		this.preferences?.registerOnSharedPreferenceChangeListener(this.preferenceListener)

		this.loadDictionaries()
	}

	override fun onCreateInputView(): View
	{
		val touchSteno = layoutInflater.inflate(R.layout.touch_steno, null)
			as TouchStenoView
		this.touchSteno = touchSteno
		touchSteno.strokeListener = this
		touchSteno.translator = this.translator

		return touchSteno
	}

	override fun onStroke(stroke: Stroke)
	{
		val ic = this.currentInputConnection
		this.translator.apply(stroke)
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
}
