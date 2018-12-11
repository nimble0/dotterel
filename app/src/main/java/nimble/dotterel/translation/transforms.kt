// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

@file:Suppress("UNUSED_PARAMETER")

package nimble.dotterel.translation

import nimble.dotterel.util.CaseInsensitiveString

val TRANSFORMS = mapOf(
	Pair("none", ::noneTransform),
	Pair("capitalise", ::capitaliseTransform),
	Pair("uncapitalise", ::uncapitaliseTransform),
	Pair("uppercase", ::upperCaseTransform),
	Pair("lowercase", ::lowerCaseTransform),
	Pair("titlecase", ::titleCaseTransform)
).mapKeys({ CaseInsensitiveString(it.key) })

fun noneTransform(context: FormattedText, text: UnformattedText, suffix: Boolean) = text

fun capitaliseTransform(
	context: FormattedText,
	text: UnformattedText,
	suffix: Boolean
) =
	if(!suffix) text.copy(text = text.text.capitalize()) else text

fun uncapitaliseTransform(
	context: FormattedText,
	text: UnformattedText,
	suffix: Boolean
) =
	if(!suffix)
		text.copy(text = text.text[0].toLowerCase().toString()
			+ text.text.substring(1))
	else
		text

fun upperCaseTransform(
	context: FormattedText,
	text: UnformattedText,
	suffix: Boolean
) =
	text.copy(text = text.text.toUpperCase())

fun lowerCaseTransform(
	context: FormattedText,
	text: UnformattedText,
	suffix: Boolean
) =
	text.copy(text = text.text.toLowerCase())

private val CAMEL_PATTERN = Regex("((?<=  ?).)")

fun titleCaseTransform(
	context: FormattedText,
	text: UnformattedText,
	suffix: Boolean
) =
	text.copy(
		text = text.text
			.replace(
				regex = CAMEL_PATTERN,
				transform = { it.value.toUpperCase() })
			.let({ if(!suffix) it.capitalize() else it }))
