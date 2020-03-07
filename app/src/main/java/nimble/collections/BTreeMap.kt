// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

import nimble.collections.facades.MutableIteratorFacade

class BTreeMap<K : Comparable<K>, V>(
	minSize: Int,
	maxSize: Int
) :
	BTreeSet<MutableMap.MutableEntry<K, V>>(minSize, maxSize, { a, b -> a.key.compareTo(b.key) }),
	MutableMap<K, V>
{
	class Entry<K, V>(
		override val key: K,
		override var value: V
	) :
		MutableMap.MutableEntry<K, V>
	{
		override fun setValue(newValue: V): V
		{
			val oldValue = this.value
			this.value = newValue
			return oldValue
		}
	}

	override fun get(key: K): V? =
		this.root.get(key, { a, b -> a.compareTo(b.key)})?.value
	override fun containsKey(key: K): Boolean = this[key] != null
	override fun containsValue(value: V): Boolean =
		this.iterator().asSequence().any({ it.value == value })

	override fun put(key: K, value: V): V?
	{
		val prev = this.root.add(
			Entry(key, value),
			this.compare)
		if(this.root.isFull)
			this.splitRoot()
		return prev?.value
	}
	override fun putIfAbsent(key: K, value: V): V?
	{
		val prev = this.root.addIfAbsent(
			Entry(key, value),
			this.compare)
		if(this.root.isFull)
			this.splitRoot()
		return prev?.value
	}
	override fun remove(key: K): V?
	{
		val v = this.root.remove(
			key,
			{ a, b -> a.compareTo(b.key) }
		)?.value
		this.removeEmptyRoot()
		return v
	}

	override fun putAll(from: Map<out K, V>) =
		from.forEach({ this[it.key] = it.value })
	override fun clear()
	{
		this.root = BTreeLeafNode(this.minSize, this.maxSize)
	}

	override fun isEmpty(): Boolean = (this.root.dataSize == 0)
	override val size: Int
		get() = this.root.size
	override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
		get() = this
	override val keys: MutableSet<K> get() = BTreeMapKeys(this)
	override val values: MutableCollection<V> get() = BTreeMapValues(this)

	override fun equals(other: Any?): Boolean
	{
		if(other === this)
			return true

		if(other is Set<*>)
			return super.equals(other)

		val other2 = other as? Map<*, *> ?: return false
		if(other2.size != size)
			return false
		for(x in this)
			if(other2[x.key] != x.value)
				return false
		return true
	}
	// Provided by BTreeSet
	override fun hashCode(): Int = super.hashCode()
}

class BTreeMapKeys<K : Comparable<K>, V>(
	val tree: BTreeMap<K, V>
) :
	MutableSet<K>
{
	override fun add(element: K): Boolean =
		throw UnsupportedOperationException()
	override fun addAll(elements: Collection<K>): Boolean =
		throw UnsupportedOperationException()

	override fun iterator(): MutableIterator<K> =
		MutableIteratorFacade(this.tree.iterator(), { it.key })

	override fun remove(element: K): Boolean = (this.tree.remove(element) != null)
	override fun removeAll(elements: Collection<K>): Boolean
	{
		var removal = false
		for(e in elements)
			removal = this.remove(e) || removal
		return removal
	}
	override fun retainAll(elements: Collection<K>): Boolean =
		this.iterator().removeIf({ it !in elements })
	override fun clear() = this.tree.clear()

	override val size: Int get() = this.tree.size
	override fun isEmpty(): Boolean = this.tree.isEmpty()

	override fun contains(element: K): Boolean = this.tree.containsKey(element)
	override fun containsAll(elements: Collection<K>): Boolean =
		elements.all({ it in this })
}

class BTreeMapValues<K : Comparable<K>, V>(
	val tree: BTreeMap<K, V>
) :
	MutableCollection<V>
{
	override fun add(element: V): Boolean =
		throw UnsupportedOperationException()
	override fun addAll(elements: Collection<V>): Boolean =
		throw UnsupportedOperationException()

	override fun iterator(): MutableIterator<V> =
		MutableIteratorFacade(this.tree.iterator(), { it.value })

	override fun remove(element: V): Boolean =
		this.iterator().removeIf({ it == element })
	override fun removeAll(elements: Collection<V>): Boolean
	{
		var removal = false
		for(e in elements)
			removal = this.remove(e) || removal
		return removal
	}
	override fun retainAll(elements: Collection<V>): Boolean =
		this.iterator().removeIf({ it !in elements })
	override fun clear() = this.tree.clear()

	override val size: Int get() = this.tree.size
	override fun isEmpty(): Boolean = this.tree.isEmpty()

	override fun contains(element: V): Boolean = this.tree.containsValue(element)
	override fun containsAll(elements: Collection<V>): Boolean =
		elements.all({ it in this })
}
