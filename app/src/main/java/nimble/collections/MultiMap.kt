// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

interface MultiMap<K, V>
{
	val size: Int
	fun isEmpty(): Boolean

	fun containsKey(key: K): Boolean
	fun containsValue(value: V): Boolean
	fun containsKeyValue(key: K, value: V): Boolean

	operator fun get(key: K): Set<V>

	val keys: Set<K>
	val values: Collection<V>
	val entries: Set<Entry<K, V>>

	interface Entry<out K, out V>
	{
		val key: K
		val value: V

		fun toPair() = Pair(this.key, this.value)
	}

	data class EntryImpl<out K, out V>(
		override val key: K,
		override val value: V
	) :
		Entry<K, V>
}

interface MutableMultiMap<K, V> : MultiMap<K, V>
{
	fun put(key: K, value: V): Boolean

	fun remove(key: K): Set<V>
	fun remove(key: K, value: V): Boolean

	fun putAll(from: MultiMap<out K, V>)
	fun clear()

	override val keys: MutableSet<K>
	override val values: MutableCollection<V>
	override val entries: MutableSet<MultiMap.Entry<K, V>>
}
