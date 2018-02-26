// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

data class Formatting(
	val spaceStart: Space = Space.NORMAL,
	// null = Carry previous spaceEnd
	val spaceEnd: Space? = Space.NORMAL,
	val space: String? = null)
{
	enum class Space
	{
		NORMAL, NONE, GLUE
	}

	fun noSpace(b: Formatting): Boolean =
		(this.spaceEnd == Space.GLUE && b.spaceStart == Space.GLUE
			|| this.spaceEnd == Space.NONE
			|| b.spaceStart == Space.NONE)

	fun withContext(context: Formatting): Formatting =
		(context + this).copy(
			spaceStart = this.spaceStart
		)

	operator fun plus(b: Formatting) =
		Formatting(
			spaceStart = this.spaceStart,
			spaceEnd = b.spaceEnd ?: this.spaceEnd,
			space = b.space ?: this.space
		)
}
