// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

fun <T> Iterable<T>.windowedWithWrapping(
	size: Int,
	step: Int = 1
): List<List<T>>
{
	val window = mutableListOf<T>()
	val result = mutableListOf<List<T>>()
	var it = this.iterator()
	for(i in 0 until size)
	{
		window.add(it.next())
		if(!it.hasNext())
			it = this.iterator()
	}

	var stepI = step
	while(it.hasNext())
	{
		if(stepI == step)
		{
			result.add(window.toList())
			stepI = 0
		}
		++stepI
		window.removeAt(0)
		window.add(it.next())
	}
	it = this.iterator()
	for(i in 0 until size - 1)
	{
		if(stepI == step)
		{
			result.add(window.toList())
			stepI = 0
		}
		++stepI
		window.removeAt(0)
		window.add(it.next())
		if(!it.hasNext())
			it = this.iterator()
	}
	if(stepI == step)
		result.add(window.toList())

	return result
}
