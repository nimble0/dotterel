// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util.ui

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned

fun SpannableStringBuilder.append(
	string: CharSequence,
	styles: List<Any>
): SpannableStringBuilder =
	this.append(SpannableString(string)
		.also({
			for(style in styles)
				it.setSpan(
					style,
					0,
					it.length,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		}))
