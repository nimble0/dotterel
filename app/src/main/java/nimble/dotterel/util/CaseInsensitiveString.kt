// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

data class CaseInsensitiveString(val value: String) : Comparable<CaseInsensitiveString>
{
	override fun equals(other: Any?): Boolean =
		this.value.equals((other as? CaseInsensitiveString)?.value, true)

	override fun compareTo(other: CaseInsensitiveString): Int =
		this.value.compareTo(other.value, true)

	override fun hashCode() =
		this.value.toLowerCase().hashCode()
}
