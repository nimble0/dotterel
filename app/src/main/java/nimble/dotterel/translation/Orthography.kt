// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import kotlin.math.max

interface Orthography
{
	data class Result(val backspaces: Int, val text: String)
	fun match(a: String, b: String): Result?
}

fun Orthography.apply(left: String, right: String): String? =
	this.match(left, right)
		?.let({ left.substring(0, max(left.length - it.backspaces, 0)) + it.text })

class NullOrthography : Orthography
{
	override fun match(a: String, b: String): Orthography.Result? = null
}

val NULL_ORTHOGRAPHY = NullOrthography()
