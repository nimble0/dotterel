// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

fun <K, V> Map<K, V>.inverted() = this.map({ Pair(it.value, it.key) }).toMap()

class BiMap<K, V>(val normal: Map<K, V>)
{
	val inverted = this.normal.inverted()
}
