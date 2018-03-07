// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

data class UnformattedText(
	val backspaces: Int = 0,
	val text: String = "",
	val formatting: Formatting = Formatting())
{
	fun format(context: FormattedText): FormattedText
	{
		var text = this

		val contextStr = context.text.substring(
			0,
			context.text.length - text.backspaces)
		var noSpace = context.formatting.noSpace(text.formatting)
		val orthography = context.formatting.orthography(text.formatting)
			?.apply(contextStr, (if(noSpace) "" else " ") + text.text)
		if(orthography != null)
			text = UnformattedText(
				text.backspaces + orthography.backspaces,
				orthography.text,
				text.formatting)

		val suffix = context.formatting.suffix(text.formatting)
		val transform = context.formatting.transform(text.formatting)
		if(transform != null)
			text = transform(context, text, suffix)

		noSpace = context.formatting.noSpace(text.formatting)
		if(context.formatting.space != null)
			text = text.copy(
				text = (if(noSpace) "" else context.formatting.space)
					+ text.text.replace(" ", context.formatting.space))

		return FormattedText(
			text.backspaces,
			text.text,
			text.formatting.withContext(context.formatting))
	}
}
