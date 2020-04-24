// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo

import kotlin.math.max
import kotlin.math.min

import nimble.collections.BTreeIterator
import nimble.collections.BTreeMap
import nimble.dotterel.translation.HistoryTranslation
import nimble.dotterel.util.fallThroughCompareTo
import nimble.dotterel.util.testBits

private const val TEXT_CONTEXT_SIZE = 1024
private const val MAX_CONTEXTS = 20

private fun findMatchingHistory(
	history: List<HistoryTranslation>,
   	context: String
): List<HistoryTranslation>
{
	val historyContext = StringBuilder()
	var historyBackspaces = 0
	var i = 0

	for(h in history.asReversed())
	{
		val backspaces = min(h.text.text.length, historyBackspaces)
		val insertText = h.text.text.substring(
			0,
			h.text.text.length - backspaces)
		historyBackspaces -= backspaces
		historyBackspaces += h.text.backspaces
		historyContext.insert(0, insertText)

		val historyContext2 = historyContext.substring(
			max(historyContext.length - context.length, 0))

		if(!context.endsWith(historyContext2))
			break

		++i
   }

   return history.subList(history.size - i, history.size)
}

private fun isPasswordField(inputType: Int): Boolean
{
	val fieldVariation = inputType and InputType.TYPE_MASK_VARIATION

	return when(inputType and InputType.TYPE_MASK_CLASS)
	{
		InputType.TYPE_CLASS_TEXT ->
			if(fieldVariation.testBits(InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT))
				fieldVariation.testBits(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
			else
				fieldVariation.testBits(InputType.TYPE_TEXT_VARIATION_PASSWORD)
		InputType.TYPE_CLASS_NUMBER ->
			fieldVariation.testBits(InputType.TYPE_NUMBER_VARIATION_PASSWORD)
		else ->
			false
	}
}

class ContextSwitcher(val dotterel: Dotterel) : Dotterel.InputStateListener
{
	private data class Editor(
		val fieldId: Int,
		val packageName: String,
		val inputType: Int,
		val reversedText: String
	) :
		Comparable<Editor>
	{
		constructor(editorInfo: EditorInfo, text: String) :
			this(
				editorInfo.fieldId,
				editorInfo.packageName ?: "",
				editorInfo.inputType,
				text.reversed())

		fun editorInfoEquals(b: EditorInfo): Boolean =
			this.fieldId == b.fieldId
				&& this.packageName == b.packageName
				&& this.inputType == b.inputType

		fun editorInfoEquals(b: Editor): Boolean =
			this.fieldId == b.fieldId
				&& this.packageName == b.packageName
				&& this.inputType == b.inputType

		override fun compareTo(other: Editor): Int =
			this.fieldId.fallThroughCompareTo(other.fieldId)
				?: this.packageName.fallThroughCompareTo(other.packageName)
				?: this.inputType.fallThroughCompareTo(other.inputType)
				?: this.reversedText.compareTo(other.reversedText)
	}

	// Sorted map to allow finding partially matched context
	private val contexts = BTreeMap<
		Editor,
		Pair<Long, List<HistoryTranslation>>
	>(10, 25)
	private var editor = Editor(EditorInfo(), "")

	private fun findMatchingContext(editor: Editor)
		: Map.Entry<Editor, Pair<Long, List<HistoryTranslation>>>?
	{
		val insertIter = this.contexts.root.find(
			editor,
			{ a, b -> a.compareTo(b.key) },
			BTreeIterator(this.contexts, mutableListOf()))
		val v = if(insertIter.hasNext()) insertIter.value() else null
		insertIter.dec()
		val v2 = if(insertIter.hasNext()) insertIter.value() else null
		return listOfNotNull(v, v2)
			.filter({ it.key.editorInfoEquals(editor) })
			.maxBy({ it.key.reversedText.commonPrefixWith(editor.reversedText).length })
			?.let({
				if(it.key.reversedText.commonPrefixWith(editor.reversedText).isEmpty())
					null
				else
					it
			})
	}

	private fun limitNumContexts()
	{
		while(this.contexts.size > MAX_CONTEXTS)
			this.contexts.remove(this.contexts.entries.minBy({ it.value.first })!!)
	}

	private fun updateContext()
	{
		Log.v("Dotterel", "Update translation history")

		val translator = this.dotterel.translator

		val text = this.dotterel.currentInputConnection
			?.getTextBeforeCursor(TEXT_CONTEXT_SIZE, 0)
			?.toString()
			?: ""
		val translatorText = translator.context.text.takeLast(TEXT_CONTEXT_SIZE)

		// Don't try changing context as long as there's at least a partial match
		// with current translation history.
		if(this.editor.editorInfoEquals(this.dotterel.currentInputEditorInfo)
			&& text.isNotEmpty()
			&& translatorText.isNotEmpty()
			&& text.endsWith(translatorText.takeLast(text.length))
		)
			return

		val newEditor = Editor(this.dotterel.currentInputEditorInfo, text)
		val matchingContext = this.findMatchingContext(newEditor)
		if(matchingContext != null)
			this.contexts.remove(matchingContext.key)
		val matchingHistory = if(matchingContext != null && text.isNotEmpty())
				findMatchingHistory(matchingContext.value.second, text)
			else
				listOf()

		val nonTextHistory = translator.history.takeLastWhile({ !it.hasText })
		val textHistory = translator.history.dropLast(nonTextHistory.size)

		if(textHistory.isNotEmpty())
		{
			this.editor = this.editor.copy(reversedText = translatorText.reversed())
			this.contexts[this.editor] = Pair(System.currentTimeMillis(), textHistory)
			this.limitNumContexts()
		}

		translator.history = (matchingHistory + nonTextHistory).toMutableList()
		this.editor = newEditor.copy(reversedText = translator.context.text.reversed())
	}

	private fun clearPasswordHistory()
	{
		if(isPasswordField(this.editor.inputType))
		{
			Log.i("Dotterel", "Clear translation history")
			this.dotterel.translator.history = this.dotterel.translator.history
				.takeLastWhile({ !it.hasText })
				.toMutableList()
			this.editor = Editor(EditorInfo(), "")
		}
	}

	override fun onStartInput(editorInfo: EditorInfo, restarting: Boolean)
	{
		Log.i("Dotterel", "Start input " +
			"${editorInfo.packageName} ${editorInfo.fieldId}/${editorInfo.inputType}")
		// Some programs don't call onFinishInput (Chrome), so clear history
		// here too if necessary.
		this.clearPasswordHistory()
		this.updateContext()
	}
	override fun onFinishInput()
	{
		Log.i("Dotterel", "Finish input " +
			"${this.editor.packageName} ${this.editor.fieldId}/${this.editor.inputType}")
		this.clearPasswordHistory()
	}
	override fun onUpdateSelection(old: IntRange, new: IntRange)
	{
		Log.v("Dotterel", "Update text box selection from $old to $new")
		if(!isPasswordField(this.dotterel.currentInputEditorInfo.inputType))
			this.updateContext()
	}
}
