// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.util.Log
import android.view.inputmethod.EditorInfo

import kotlin.math.max
import kotlin.math.min

import nimble.collections.BTreeIterator
import nimble.collections.BTreeMap
import nimble.dotterel.translation.HistoryTranslation
import nimble.dotterel.util.fallThroughCompareTo

private const val TEXT_CONTEXT_SIZE = 1024

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

class ContextSwitcher(val dotterel: Dotterel) : Dotterel.InputStateListener
{
	private data class Editor(
		val fieldId: Int,
		val packageName: String,
		val inputType: Int,
		val text: String
	) :
		Comparable<Editor>
	{
		constructor(editorInfo: EditorInfo, text: String) :
			this(
				editorInfo.fieldId,
				editorInfo.packageName ?: "",
				editorInfo.inputType,
				text)

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
				?: this.text.reversed().compareTo(other.text.reversed())
	}

	// Sorted map to allow finding partially matched context
	private val contexts = BTreeMap<Editor, MutableList<HistoryTranslation>>(10, 25)
	private var editor = Editor(EditorInfo(), "")

	private fun findMatchingContext(editor: Editor)
		: Map.Entry<Editor, List<HistoryTranslation>>?
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
			.maxBy({ it.key.text.commonSuffixWith(editor.text).length })
			?.let({
				if(it.key.text.commonSuffixWith(editor.text).isEmpty())
					null
				else
					it
			})
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
				findMatchingHistory(matchingContext.value, text)
			else
				listOf()

		if(translator.history.isNotEmpty())
		{
			this.editor = this.editor.copy(text = translatorText)
			this.contexts[this.editor] = translator.history
		}

		translator.history = matchingHistory.toMutableList()
		this.editor = newEditor.copy(text = translator.context.text)
	}

	override fun onStartInput(editorInfo: EditorInfo, restarting: Boolean)
	{
		Log.i("Dotterel", "Start input " +
			"${editorInfo.packageName} ${editorInfo.fieldId}/${editorInfo.inputType}")
		this.updateContext()
	}
	override fun onFinishInput()
	{
		Log.i("Dotterel", "Finish input " +
			"${this.editor.packageName} ${this.editor.fieldId}/${this.editor.inputType}")
	}
	override fun onUpdateSelection(old: IntRange, new: IntRange)
	{
		Log.v("Dotterel", "Update text box selection from $old to $new")
		this.updateContext()
	}
}
