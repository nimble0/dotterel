// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

fun <T> MutableIterator<T>.removeIf(predicate: (T) -> Boolean): Boolean
{
	var changed = false
	while(this.hasNext())
	{
		if(predicate(this.next()))
		{
			this.remove()
			changed = true
		}
	}
	return changed
}
