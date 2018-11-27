// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

fun product(counts: List<Int>): Sequence<List<Int>>
{
	if(counts.any({ it == 0 }))
		return generateSequence({ null })

	val indices = MutableList(counts.size, { 0 })
	indices[indices.lastIndex] = -1
	return generateSequence({
		var i = indices.lastIndex
		while(i >= 0)
		{
			++indices[i]
			if(indices[i] < counts[i])
				break
			indices[i] = 0

			--i
		}

		if(i == -1)
			null
		else
			indices.toList()
	})
}
