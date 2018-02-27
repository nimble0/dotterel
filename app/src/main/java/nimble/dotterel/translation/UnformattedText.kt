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

		val suffix = context.formatting.suffix(text.formatting)
		val transform = context.formatting.transform(text.formatting)
		if(transform != null)
			text = transform(context, text, suffix)

		val noSpace = context.formatting.noSpace(text.formatting)
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
