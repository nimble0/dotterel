// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections.facades

class IteratorFacade<T, T2>(
	val iterator: Iterator<T>,
	val transform: (T) -> T2
) :
	Iterator<T2>
{
	override fun hasNext(): Boolean = this.iterator.hasNext()
	override fun next(): T2 = this.transform(this.iterator.next())
}

class MutableIteratorFacade<T, T2>(
	val iterator: MutableIterator<T>,
	val transform: (T) -> T2
) :
	MutableIterator<T2>
{
	override fun hasNext(): Boolean = this.iterator.hasNext()
	override fun next(): T2 = this.transform(this.iterator.next())
	override fun remove() = this.iterator.remove()
}
