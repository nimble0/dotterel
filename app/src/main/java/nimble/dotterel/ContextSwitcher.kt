// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.util.Log
import android.view.inputmethod.EditorInfo

import kotlin.math.max
import kotlin.math.min

import nimble.dotterel.translation.HistoryTranslation

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
		val inputType: Int)
	{
		constructor(editorInfo: EditorInfo) :
			this(
				editorInfo.fieldId,
				editorInfo.packageName ?: "",
				editorInfo.inputType)
	}

	private val contexts = mutableMapOf<Editor, MutableList<HistoryTranslation>>()
	private var editor = Editor(EditorInfo())

	private fun updateContext()
	{
		Log.v("Dotterel", "Update translation history")

		val translator = this.dotterel.translator

		if(translator.history.isNotEmpty())
			this.contexts[this.editor] = translator.history

		val text = this.dotterel.currentInputConnection
			?.getTextBeforeCursor(TEXT_CONTEXT_SIZE, 0)
			?.toString()
			?: ""
		val newEditor = Editor(this.dotterel.currentInputEditorInfo)
		val matchingHistory = if(text.isNotEmpty())
				findMatchingHistory(
					this.contexts[newEditor] ?: listOf(),
					text)
			else
				listOf()

		translator.history = matchingHistory.toMutableList()
		this.editor = newEditor
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
