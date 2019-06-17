// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.text.*
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*

import androidx.appcompat.widget.AppCompatImageButton

import nimble.dotterel.translation.*
import nimble.dotterel.util.CaseInsensitiveString
import nimble.dotterel.util.Vector2
import nimble.dotterel.util.ui.FloatingDialog

private fun relatedTranslations(translation: String) = listOf(
	translation,        // Same
	"{^$translation}",  // Prefix
	"{^}$translation",
	"{^$translation^}", // Infix
	"{^}$translation{^}",
	"{$translation^}",  // Suffix
	"$translation{^}",
	"{&$translation}",  // Fingerspell
	"{#$translation}"   // Command
)

class LookupTranslationDialog(dotterel: Dotterel, translator: Translator) :
	FloatingDialog(
		dotterel,
		R.layout.lookup_translation,
		"tool/lookupTranslation",
		Vector2(200f, 150f))
{
	init
	{
		val system = translator.system
		val dictionaries = system.dictionaries.dictionaries
			.mapNotNull({ dictionary ->
				(dictionary.dictionary as? ReverseDictionary)
					?.let({ Pair(dictionary.path, it) })
			})

		val translationBox = this.view.findViewById<EditText>(R.id.translation)
		val lookupResultsBox = this.view.findViewById<TextView>(R.id.lookup_results)
		lookupResultsBox.movementMethod = ScrollingMovementMethod()

		translationBox.addTextChangedListener(object : TextWatcher{
			override fun afterTextChanged(s: Editable?) = Unit
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)
			{
				val translation = translationBox.text.toString().trim(' ')
				val lookupTranslations = relatedTranslations(translation)
					.map({ CaseInsensitiveString(it) })

				lookupResultsBox.text = dictionaries
					.map({ dictionary ->
						lookupTranslations.map({ dictionary.second.findTranslations(it) })
							.flatten()
							.map({ translation ->
								val results = dictionary.second.reverseGet(translation)
									.joinToString(
										separator = "\n\t",
										prefix = "\n\t",
										transform = { it.rtfcre })
								"$translation: (${dictionary.first})$results"
							})
					})
					.flatten()
					.joinToString(separator = "\n")
			}
		})

		this.view.findViewById<AppCompatImageButton>(android.R.id.closeButton)
			.setOnClickListener({ this.close() })

		translationBox.setOnKeyListener({ _, keyCode, event ->
			if(event.action == KeyEvent.ACTION_DOWN)
			{
				when(keyCode)
				{
					KeyEvent.KEYCODE_BACK,
					KeyEvent.KEYCODE_ESCAPE,
					KeyEvent.KEYCODE_ENTER ->
					{
						this.close()
						true
					}
					else -> false
				}
			}
			else
				false
		})

		translationBox.setOnEditorActionListener { _, actionId, _ ->
			if(actionId == EditorInfo.IME_ACTION_DONE)
			{
				this.close()
				true
			}
			else
				false
		}
	}
}

@Suppress("UNUSED_PARAMETER")
fun lookupTranslation(dotterel: Dotterel, translator: Translator, arg: String)
{
	LookupTranslationDialog(dotterel, translator)
}
