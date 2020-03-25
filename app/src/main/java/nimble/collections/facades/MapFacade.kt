// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections.facades

class MapFacade<K, V, K2, V2>(
	val map: Map<K2, V2>,
	val transformKey: (K) -> K2,
	val transformKeyBack: (K2) -> K,
	val transformValue: (V) -> V2,
	val transformValueBack: (V2) -> V
) :
	Map<K, V>
{
	data class Entry<K, V>(
		override val key: K,
		override val value: V
	) :
		Map.Entry<K, V>

	override val entries: Set<Map.Entry<K, V>>
		get() = SetFacade(
			this.map.entries,
			{ Entry(this.transformKey(it.key), this.transformValue(it.value)) },
			{ Entry(this.transformKeyBack(it.key), this.transformValueBack(it.value)) })
	override val keys: Set<K>
		get() = SetFacade(
			this.map.keys,
			this.transformKey,
			this.transformKeyBack)
	override val values: Collection<V>
		get() = CollectionFacade(
			this.map.values,
			this.transformValue,
			this.transformValueBack)

	override fun containsKey(key: K): Boolean =
		this.map.containsKey(this.transformKey(key))
	override fun containsValue(value: V): Boolean =
		this.map.containsValue(this.transformValue(value))

	override fun get(key: K): V? = this.map[this.transformKey(key)]
		?.let({ this.transformValueBack(it) })

	override val size: Int get() = this.map.size
	override fun isEmpty(): Boolean = this.map.isEmpty()
}
