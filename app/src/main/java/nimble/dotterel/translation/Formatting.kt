// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

data class Formatting(
	val space: String? = null,
	val spaceStart: Space = Space.NORMAL,
	// null = Carry previous spaceEnd
	val spaceEnd: Space? = Space.NORMAL,

	// null = Carry previous orthography
	val orthography: Orthography? = null,
	val orthographyStart: Boolean = true,
	// null = Carry previous orthographyEnd
	val orthographyEnd: Boolean? = true,

	// null = Carry previous transform
	var transform: ((
		context: FormattedText,
		text: UnformattedText,
		suffix: Boolean
	) -> UnformattedText)? = null,
	var singleTransform: ((
		context: FormattedText,
		text: UnformattedText,
		suffix: Boolean
	) -> UnformattedText)? = null,
	var transformState: TransformState = TransformState.NORMAL)
{
	enum class Space
	{
		NORMAL, NONE, GLUE
	}

	enum class TransformState
	{
		// Transform decays (main body to suffix then back to normal
		// (and single transform resets))
		NORMAL,
		MAIN, // Normal transform
		SUFFIX, // Transform suffix. Eg/ Uppercase {^ing} = ING, Capitialised {^ing} = ing
		CARRY // Transform doesn't decay
	}

	fun noSpace(b: Formatting): Boolean =
		(this.spaceEnd == Space.GLUE && b.spaceStart == Space.GLUE
			|| this.spaceEnd == Space.NONE
			|| b.spaceStart == Space.NONE)

	fun orthography(b: Formatting): Orthography? =
		if(this.orthographyEnd == true && b.orthographyStart &&
			(b.orthography == null || this.orthography == b.orthography))
			this.orthography
		else
			null

	fun suffix(b: Formatting) =
		this.transformState == TransformState.SUFFIX && this.noSpace(b)

	fun transform(b: Formatting): ((
		context: FormattedText,
		text: UnformattedText,
		suffix: Boolean
	) -> UnformattedText)? =
		if(this.singleTransform != null
			&& (this.transformState == TransformState.MAIN
				|| this.transformState == TransformState.SUFFIX
					&& this.noSpace(b)))
			this.singleTransform
		else
			this.transform

	fun withContext(context: Formatting): Formatting =
		(context + this).copy(
			spaceStart = this.spaceStart,
			orthographyStart = this.orthographyStart
		)

	operator fun plus(b: Formatting): Formatting
	{
		var singleTransform: ((
			context: FormattedText,
			text: UnformattedText,
			suffix: Boolean
		) -> UnformattedText)? = this.singleTransform
		var transformState = this.transformState

		when(b.transformState)
		{
			TransformState.CARRY -> {}
			TransformState.NORMAL ->
			{
				when(this.transformState)
				{
					TransformState.MAIN ->
						transformState = TransformState.SUFFIX
					TransformState.NORMAL,
					TransformState.SUFFIX ->
						if(!this.noSpace(b))
						{
							singleTransform = null
							transformState = TransformState.MAIN
						}
					TransformState.CARRY ->
					{
						singleTransform = null
						transformState = TransformState.NORMAL
					}
				}
			}
			else ->
			{
				singleTransform = b.singleTransform
				transformState = b.transformState
			}
		}

		return Formatting(
			space = b.space ?: this.space,
			spaceStart = this.spaceStart,
			spaceEnd = b.spaceEnd ?: this.spaceEnd,
			orthography = b.orthography ?: this.orthography,
			orthographyStart = this.orthographyStart,
			orthographyEnd = b.orthographyEnd,
			transform = b.transform ?: this.transform,
			singleTransform = singleTransform,
			transformState = transformState)
	}
}
