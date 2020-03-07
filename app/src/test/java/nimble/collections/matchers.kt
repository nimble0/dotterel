// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

import io.kotlintest.matchers.*

fun <T> setContains(x: T) = object : Matcher<Set<T>>
{
	override fun test(value: Set<T>) = Result(
		value.contains(x),
		"Set $value should include $x")
}

infix fun <T> Set<T>.shouldContain(x: T) = this should setContains(x)
infix fun <T> Set<T>.shouldNotContain(x: T) = this shouldNot setContains(x)
