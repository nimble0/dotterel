// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections.facades

class CollectionFacade<T, T2>(
	val collection: Collection<T2>,
	val transform: (T) -> T2,
	val transformBack: (T2) -> T
) :
	Collection<T>
{
	override fun contains(element: T): Boolean =
		this.collection.contains(this.transform(element))
	override fun containsAll(elements: Collection<T>): Boolean =
		elements.all({ this.collection.contains(this.transform(it)) })

	override val size: Int get() = this.collection.size
	override fun isEmpty(): Boolean = this.collection.isEmpty()

	override fun iterator(): Iterator<T> =
		IteratorFacade(this.collection.iterator(), this.transformBack)
}
