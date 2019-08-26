// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

fun lerp(a: Float, b: Float, i: Float) = a * (1 - i) + b * i
