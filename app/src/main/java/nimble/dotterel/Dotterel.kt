// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View

import java.io.IOException

import nimble.dotterel.machines.TouchStenoView
import nimble.dotterel.translation.*
import nimble.dotterel.translation.systems.IRELAND_SYSTEM

val CODE_ASSETS = mapOf(
	Pair("dictionaries/Numbers", NumbersDictionary())
)

class Dotterel : InputMethodService(), StrokeListener
{
	private var touchSteno: TouchStenoView? = null
	private var translator = Translator(
		IRELAND_SYSTEM,
		log = { m -> Log.i("Steno", m) })

	fun getDictionary(path: String): Dictionary?
	{
		try
		{
			val type = path.substringBefore("://")
			val name = path.substringAfter("://")
			return when(type)
			{
				"asset" -> JsonDictionary(this.assets.open(name))
				"code" -> CODE_ASSETS[name] as Dictionary
				else -> null
			}
		}
		catch(e: IOException)
		{
			Log.i("IO", "Error reading dictionary $path")
			return null
		}
		catch(e: ClassCastException)
		{
			Log.i("Type", "$path is not of type Dictionary")
			return null
		}
	}

	override fun onCreateInputView(): View
	{
		val touchSteno = layoutInflater.inflate(R.layout.touch_steno, null)
			as TouchStenoView
		this.touchSteno = touchSteno
		touchSteno.strokeListener = this

		this.translator.dictionary = MultiDictionary(
			this.translator.system.defaultDictionaries
				.mapNotNull({ this.getDictionary(it) }))

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
