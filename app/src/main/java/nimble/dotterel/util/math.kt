// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import kotlin.math.max
import kotlin.math.min

fun clamp(v: Int, minimum: Int, maximum: Int) = min(max(v, minimum), maximum)
fun clamp(v: Long, minimum: Long, maximum: Long) = min(max(v, minimum), maximum)
fun clamp(v: Float, minimum: Float, maximum: Float) = min(max(v, minimum), maximum)
fun clamp(v: Double, minimum: Double, maximum: Double) = min(max(v, minimum), maximum)
