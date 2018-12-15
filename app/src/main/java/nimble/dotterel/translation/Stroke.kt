// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

data class Stroke(val layout: KeyLayout, val keys: Long)
{
	operator fun plus(b: Stroke) = Stroke(this.layout, this.keys or b.keys)
	operator fun minus(b: Stroke) = Stroke(this.layout, this.keys and b.keys.inv())

	val rtfcre: String get() = this.layout.rtfcre(this.keys)
	val pureKeysString: String get() = this.layout.pureKeysString(this.keys)

	override fun toString() = this.rtfcre
}

val List<Stroke>.rtfcre: String
	get() = this.joinToString(transform = { it.rtfcre }, separator = "/")
