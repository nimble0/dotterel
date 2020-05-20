// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.graphics.Color
import android.graphics.Typeface
import android.text.*
import android.text.method.ScrollingMovementMethod
import android.text.style.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*

import androidx.appcompat.widget.AppCompatImageButton

import androidx.core.graphics.ColorUtils

import nimble.dotterel.translation.*
import nimble.dotterel.util.CaseInsensitiveString
import nimble.dotterel.util.Vector2
import nimble.dotterel.util.ui.append
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

private data class LookupResult(
	val dictionary: String,
	val translation: String,
	val strokes: Set<List<Stroke>>
)

private fun findResults(
	translation: String,
	dictionaries: List<Pair<String, ReverseDictionary>>
): List<LookupResult>
{
	val lookupTranslations = relatedTranslations(translation)
		.map({ CaseInsensitiveString(it) })

	return dictionaries
		.map({ d ->
			lookupTranslations.map({ d.second.findTranslations(it) })
				.flatten()
				.map({ LookupResult(
					d.first,
					it,
					d.second.reverseGet(it))
				})
				.distinct()
		})
		.flatten()
}

private fun formatResults(
	results: List<LookupResult>,
	dictionary: Dictionary,
	disabledColour: Int
): Spannable
{
	val overwrittenStyle = {
		listOf(
			ForegroundColorSpan(disabledColour),
			StrikethroughSpan())
	}
	val translationStyle = { listOf<Spannable>() }
	val dictionaryStyle = {
		listOf(
			StyleSpan(Typeface.ITALIC),
			ForegroundColorSpan(disabledColour))
	}
	val strokesStyle = {
		listOf(
			TypefaceSpan("monospace"),
			StyleSpan(Typeface.BOLD))
	}

	val shownTranslations = mutableSetOf<Pair<String, String>>()

	val isOverwritten = { strokes: List<Stroke>, translation: String ->
		dictionary[strokes] != translation ||
			Pair(strokes.rtfcre, translation) in shownTranslations
	}

	return results.fold(SpannableStringBuilder(), { acc, entry ->
		val allOverwritten = entry.strokes.all({ strokes -> isOverwritten(strokes, entry.translation) })

		acc.append(
			entry.translation,
			translationStyle() + if(allOverwritten) overwrittenStyle() else listOf())
		acc.append(
			" (${entry.dictionary})\n",
			dictionaryStyle() + if(allOverwritten) overwrittenStyle() else listOf())
		for(strokes in entry.strokes)
		{
			val overwritten = isOverwritten(strokes, entry.translation)
			acc.append(
				"\t${strokes.rtfcre}\n",
				strokesStyle() + if(overwritten) overwrittenStyle() else listOf())
		}

		shownTranslations.addAll(
			entry.strokes.map({ Pair(it.rtfcre, entry.translation) })
		)

		acc
	})
}

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
				val disabledColour = ColorUtils.blendARGB(
					Color.TRANSPARENT,
					lookupResultsBox.currentTextColor,
					0.84f)

				val results = findResults(
					translationBox.text.toString().trim(' '),
					dictionaries)

				lookupResultsBox.text = formatResults(
					results,
					system.dictionaries,
					disabledColour)
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
