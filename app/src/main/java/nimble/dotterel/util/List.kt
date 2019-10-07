// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

fun <T> List<T>.rotate(n: Int): List<T>
{
	val n2 = n % this.size
	return when
	{
		n2 > 0 -> this.drop(n2) + this.take(n2)
		n2 < 0 -> this.takeLast(-n2) + this.dropLast(-n2)
		else -> this
	}
}
