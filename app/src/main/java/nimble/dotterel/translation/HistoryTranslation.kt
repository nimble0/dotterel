// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

class HistoryTranslation(
	val translation: Translation,
	context: FormattedText,
	val actions: List<Any>)
{
	val replacesText: String
	val text: String
	val formatting: Formatting

	init
	{
		var actionsText: FormattedText? = null
		for(a in actions)
			if(a is FormattedText)
				if(actionsText == null)
					actionsText = a
				else
					actionsText += a

		if(actionsText == null)
			actionsText = FormattedText(0, "", context.formatting)

		this.replacesText = context.text.substring(
			context.text.length - actionsText.backspaces)
		this.text = actionsText.text
		this.formatting = actionsText.formatting
	}

	val undoable: Boolean = !(this.replacesText.isEmpty() && this.text.isEmpty())

	val undoAction = FormattedText(this.text.length, this.replacesText, Formatting())
	val redoAction = FormattedText(this.replacesText.length, this.text, this.formatting)
	val undoReplacedAction: FormattedText =
		this.translation.replaces.fold(FormattedText(0, "", Formatting()),
			{ acc, it -> acc + it.undoAction })
	val redoReplacedAction: FormattedText =
		this.translation.replaces.fold(FormattedText(0, "", Formatting()),
			{ acc, it -> acc + it.redoAction })
}
