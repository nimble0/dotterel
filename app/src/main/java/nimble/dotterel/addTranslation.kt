// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*

import androidx.appcompat.widget.AppCompatImageButton

import nimble.dotterel.translation.*
import nimble.dotterel.util.Vector2
import nimble.dotterel.util.ui.FloatingDialog

private fun lastWord(translator: Translator): String
{
	if(translator.history.isEmpty())
		return ""

	val space = translator.history.last().text.formatting.space!!
	val lastWord = StringBuilder()
	for(text in translator.history.asReversed().map({ it.text.text }))
	{
		val spaceI = text.indexOf(space)
		if(spaceI != -1)
		{
			val last = text.substring(spaceI + space.length)
			lastWord.insert(0, last)
			break
		}
		else
			lastWord.insert(0, text)
	}

	return lastWord.toString()
}

class AddTranslationDialog(
	dotterel: Dotterel,
	translator: Translator,
	initialDictionary: String,
	initialTranslation: String
) :
	FloatingDialog(
		dotterel,
		R.layout.add_translation,
		"tool/addTranslation",
		Vector2(200f, 150f))
{
	private val system: System = translator.system
	private val dictionaries: List<SystemDictionary> =
		this.system.dictionaries.dictionaries
			.filter({ it.dictionary is SaveableDictionary })

	private val dictionariesSpinner =
		this.view.findViewById<Spinner>(R.id.dictionary)
			.also({ spinner ->
				spinner.adapter = ArrayAdapter<String>(
					dotterel,
					R.layout.add_translation_spinner,
					dictionaries.map({ it.path })
				).also({
					it.setDropDownViewResource(R.layout.add_translation_spinner_dropdown)
				})
			})
	private val strokesBox = this.view.findViewById<EditText>(R.id.strokes)
	private val translationBox = this.view.findViewById<EditText>(R.id.translation)

	init
	{
		val selected = dictionaries.indexOfFirst({ it.path == initialDictionary })
		if(selected != -1)
			this.dictionariesSpinner.setSelection(selected)

		this.dictionariesSpinner.isFocusable = true
		this.dictionariesSpinner.isFocusableInTouchMode = true

		val overwriteEntry = view.findViewById<TextView>(R.id.overwrite_entry)
		val existingEntries = view.findViewById<TextView>(R.id.existing_entries)
		existingEntries.movementMethod = ScrollingMovementMethod()

		this.strokesBox.addTextChangedListener(object : TextWatcher
		{
			override fun afterTextChanged(s: Editable?) = Unit
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)
			{
				val strokes = system.keyLayout.parse(
					this@AddTranslationDialog.strokesBox.text.toString().trim().split(" "))
				overwriteEntry.text = system.dictionaries[strokes]
			}
		})

		this.translationBox.addTextChangedListener(object : TextWatcher
		{
			override fun afterTextChanged(s: Editable?) = Unit
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)
			{
				val translation = this@AddTranslationDialog.translationBox.text.toString().trim()
				existingEntries.text = this@AddTranslationDialog.system.dictionaries.dictionaries
					.mapNotNull({ (it.dictionary as? ReverseDictionary) })
					.map({ it.reverseGet(translation) })
					.flatten()
					.filter({ system.dictionaries[it] == translation })
					.joinToString(transform = { it.rtfcre })
			}
		})

		this.translationBox.setText(initialTranslation)

		this.view.findViewById<AppCompatImageButton>(android.R.id.closeButton)
			.setOnClickListener({ this.close() })

		this.view.findViewById<Button>(R.id.cancel)
			.setOnClickListener({ this.close() })
		this.view.findViewById<Button>(R.id.ok)
			.setOnClickListener({ this.doAddTranslation() })

		val keyListener = { _: View, keyCode: Int, event: KeyEvent ->
			if(event.action == KeyEvent.ACTION_DOWN)
			{
				when(keyCode)
				{
					KeyEvent.KEYCODE_BACK,
					KeyEvent.KEYCODE_ESCAPE ->
					{
						this.close()
						true
					}
					KeyEvent.KEYCODE_ENTER ->
					{
						this.doAddTranslation()
						true
					}
					else -> false
				}
			}
			else
				false
		}
		this.dictionariesSpinner.setOnKeyListener(keyListener)
		this.strokesBox.setOnKeyListener(keyListener)
		this.translationBox.setOnKeyListener(keyListener)
		existingEntries.setOnKeyListener(keyListener)

		val editorActionListener = { _: TextView, actionId: Int, _: KeyEvent? ->
			if(actionId == EditorInfo.IME_ACTION_DONE)
			{
				this.doAddTranslation()
				true
			}
			else
				false
		}
		this.strokesBox.setOnEditorActionListener(editorActionListener)
		this.translationBox.setOnEditorActionListener(editorActionListener)
	}

	private fun doAddTranslation()
	{
		this.close()

		val systemDictionary = this.dictionaries
			.getOrNull(dictionariesSpinner.selectedItemPosition)
		if(systemDictionary == null)
		{
			Toast.makeText(dotterel, "No dictionary selected", Toast.LENGTH_LONG).show()
			return
		}
		if(systemDictionary.dictionary !is MutableDictionary)
		{
			Toast.makeText(dotterel, "Invalid dictionary selected", Toast.LENGTH_LONG).show()
			return
		}

		val strokes = this.system.keyLayout.parse(
				this.strokesBox.text.toString().trim().split(" "))
			.filterNot({ it.isEmpty })
		val translation = this.translationBox.text.toString().trim()

		if(strokes.isEmpty())
		{
			Toast.makeText(dotterel, "Empty stroke sequence", Toast.LENGTH_LONG).show()
			return
		}

		systemDictionary.dictionary[strokes] = translation
		this.system.saveDictionary(systemDictionary)
	}
}

fun addTranslation(dotterel: Dotterel, translator: Translator, arg: String)
{
	AddTranslationDialog(dotterel, translator, arg, lastWord(translator))
}
