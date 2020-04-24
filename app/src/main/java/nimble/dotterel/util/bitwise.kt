// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

fun Int.testBits(bits: Int) = (this and bits == bits)
fun Long.testBits(bits: Long) = (this and bits == bits)
