// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

import nimble.collections.facades.MutableIteratorFacade

class BasicMultiMap<K, V> : MutableMultiMap<K, V>
{
	val data: MutableMap<K, MutableSet<V>> = mutableMapOf()

	override val size: Int get() = this.data.values.sumBy({ it.size })
	override fun isEmpty(): Boolean = this.data.isEmpty()

	override fun containsKey(key: K): Boolean = this.data.containsKey(key)
	override fun containsValue(value: V): Boolean =
		this.data.values.any({ value in it })
	override fun containsKeyValue(key: K, value: V): Boolean =
		this.data[key]?.contains((value)) ?: false

	override fun get(key: K): Set<V> = this.data[key] ?: emptySet()

	override fun put(key: K, value: V): Boolean =
		this.data.getOrPut(key, { mutableSetOf() }).add(value)
	override fun putAll(from: MultiMap<out K, V>) =
		from.entries.forEach({ this.put(it.key, it.value) })

	override fun remove(key: K): Set<V> = this.data.remove(key) ?: emptySet()
	override fun remove(key: K, value: V): Boolean
	{
		val values = this.data[key] ?: return false
		val removed = values.remove(value)
		if(values.isEmpty())
			this.data.remove(key)
		return removed
	}
	override fun clear() = this.data.clear()

	override val keys: MutableSet<K> get() = this.data.keys
	override val values: MutableCollection<V>
		get() = BasicMultiMapValues(this)
	override val entries: MutableSet<MultiMap.Entry<K, V>>
		get() = BasicMultiMapEntries(this)

	override fun equals(other: Any?): Boolean
	{
		if(other === this)
			return true

		if(other is Set<*>)
			return super.equals(other)

		@Suppress("UNCHECKED_CAST")
		val other2 = other as? MultiMap<K, V> ?: return false
		if(other2.size != size)
			return false
		for(key in this.keys)
			if(this[key] == other2[key])
				return false
		return true
	}
	override fun hashCode(): Int = data.hashCode()
}

class BasicMultiMapIterator<K, V>(
	private val keyIter: MutableIterator<Map.Entry<K, MutableSet<V>>>
) :
	MutableIterator<MultiMap.Entry<K, V>>
{
	private var entry: Map.Entry<K, MutableSet<V>>? = null
	private var valueIter: MutableIterator<V>? = null

	override fun hasNext(): Boolean
	{
		if(this.valueIter?.hasNext() != true && this.keyIter.hasNext())
		{
			val next = this.keyIter.next()
			this.entry = next
			this.valueIter = next.value.iterator()
		}

		return this.valueIter?.hasNext() == true
	}

	override fun next(): MultiMap.Entry<K, V> =
		MultiMap.EntryImpl(this.entry!!.key, this.valueIter!!.next())

	override fun remove()
	{
		this.valueIter!!.remove()
		if(this.entry!!.value.isEmpty())
			this.keyIter.remove()
	}
}

class BasicMultiMapValues<K, V>(
	val map: BasicMultiMap<K, V>
) :
	MutableCollection<V>
{
	override fun add(element: V): Boolean =
		throw UnsupportedOperationException()
	override fun addAll(elements: Collection<V>): Boolean =
		throw UnsupportedOperationException()

	override fun contains(element: V): Boolean = this.map.containsValue(element)
	override fun containsAll(elements: Collection<V>): Boolean =
		elements.all({ this.map.containsValue(it) })

	override fun iterator(): MutableIterator<V> =
		MutableIteratorFacade(BasicMultiMapIterator(this.map.data.iterator()), { it.value })

	override fun remove(element: V): Boolean =
		this.iterator().removeIf({ it == element })
	override fun removeAll(elements: Collection<V>): Boolean =
		this.iterator().removeIf({ it in elements })
	override fun retainAll(elements: Collection<V>): Boolean =
		this.iterator().removeIf({ it !in elements })
	override fun clear() = this.map.clear()

	override val size: Int get() = this.map.size
	override fun isEmpty(): Boolean = this.map.isEmpty()
}

class BasicMultiMapEntries<K, V>(
	val map: BasicMultiMap<K, V>
) :
	MutableSet<MultiMap.Entry<K, V>>
{
	override fun add(element: MultiMap.Entry<K, V>): Boolean =
		this.map.put(element.key, element.value) != element.value
	override fun addAll(elements: Collection<MultiMap.Entry<K, V>>): Boolean
	{
		var changed = false
		for(x in elements)
			changed = this.add(x) || changed
		return changed
	}

	override fun contains(element: MultiMap.Entry<K, V>): Boolean =
		this.map.containsKeyValue(element.key, element.value)
	override fun containsAll(elements: Collection<MultiMap.Entry<K, V>>): Boolean =
		elements.all({ it in this })

	override fun iterator(): MutableIterator<MultiMap.Entry<K, V>> =
		BasicMultiMapIterator(this.map.data.iterator())

	override fun remove(element: MultiMap.Entry<K, V>): Boolean =
		this.map.remove(element.key, element.value)
	override fun removeAll(elements: Collection<MultiMap.Entry<K, V>>): Boolean
	{
		var removal = false
		for(e in elements)
			removal = this.remove(e) || removal
		return removal
	}
	override fun retainAll(elements: Collection<MultiMap.Entry<K, V>>): Boolean =
		this.iterator().removeIf({ it !in elements })
	override fun clear() = this.map.clear()

	override val size: Int get() = this.map.size
	override fun isEmpty(): Boolean = this.map.isEmpty()
}
