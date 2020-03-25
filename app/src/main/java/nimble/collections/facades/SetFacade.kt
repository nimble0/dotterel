// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections.facades

class SetFacade<T, T2>(
	val set: Set<T2>,
	val transform: (T) -> T2,
	val transformBack: (T2) -> T
) :
	Set<T>
{
	override fun contains(element: T): Boolean = this.set.contains(this.transform(element))
	override fun containsAll(elements: Collection<T>): Boolean =
		elements.all({ this.set.contains(this.transform(it)) })

	override val size: Int get() = this.set.size
	override fun isEmpty(): Boolean = this.set.isEmpty()

	override fun iterator(): Iterator<T> =
		IteratorFacade(this.set.iterator(), this.transformBack)
}
