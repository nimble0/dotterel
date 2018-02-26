// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

data class FormattedText(
	val backspaces: Int = 0,
	val text: String = "",
	val formatting: Formatting = Formatting())
{
	operator fun plus(b: FormattedText): FormattedText
	{
		var backspaces = this.backspaces
		var text = ""

		val l = this.text.length - b.backspaces
		if(l >= 0)
			text = this.text.substring(0, l)
		else
			backspaces += -l

		text += b.text

		return FormattedText(backspaces, text, this.formatting + b.formatting)
	}
}
