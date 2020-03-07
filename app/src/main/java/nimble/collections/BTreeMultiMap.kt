// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

import nimble.collections.facades.MutableIteratorFacade

class BTreeMultiMap<K : Comparable<K>, V : Comparable<V>>(
	minSize: Int,
	maxSize: Int
) :
	BTreeSet<MultiMap.Entry<K, V>>(
		minSize,
		maxSize,
		{ a, b ->
			val compare = a.key.compareTo(b.key)
			if(compare != 0) compare else a.value.compareTo(b.value)
		}
	),
	MutableMultiMap<K, V>
{
	class Entry<K, V>(
		override var key: K,
		override var value: V
	) :
		MultiMap.Entry<K, V>

	override fun containsKey(key: K): Boolean =
		(this.root.get(key, { a, b -> a.compareTo(b.key) }) != null)
	override fun containsValue(value: V): Boolean =
		this.values.contains(value)
	override fun containsKeyValue(key: K, value: V): Boolean =
		this.contains(Entry(key, value))

	override fun get(key: K): Set<V> =
		BTreeMultiMapKeyIterator(
			this.root.find(
				key,
				{ a, b -> if(a <= b.key) -1 else 1 },
				BTreeIterator(this, mutableListOf())),
			key
		)
			.asSequence()
			.map({ it.value })
			.toSet()

	override fun put(key: K, value: V) =
		this.add(Entry(key, value))
	override fun putAll(from: MultiMap<out K, V>) =
		from.entries.forEach({ this.put(it.key, it.value) })
	override fun remove(key: K): Set<V>
	{
		val values = this[key]
		for(v in values)
			this.remove(key, v)
		return values
	}
	override fun remove(key: K, value: V): Boolean = this.remove(Entry(key, value))

	override val keys: MutableSet<K>
		get() = BTreeMultiMapKeys(this)
	override val values: MutableCollection<V>
		get() = BTreeMultiMapValues(this)
	override val entries: MutableSet<MultiMap.Entry<K, V>>
		get() = this

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
		for(x in this)
			if(!other2.containsKeyValue(x.key, x.value))
				return false
		return true
	}
	// Provided by BTreeSet
	override fun hashCode(): Int = super.hashCode()
}

class BTreeMultiMapKeyIterator<K, V>(
	val iterator: BTreeIterator<MultiMap.Entry<K, V>>,
	val key: K
) :
	MutableIterator<MultiMap.Entry<K, V>>
{
	operator fun inc(): BTreeMultiMapKeyIterator<K, V>
	{
		this.iterator.inc()
		return this
	}
	fun value() = this.iterator.value()

	override fun hasNext() = this.iterator.hasNext() && this.value().key == this.key
	override fun next() = this.iterator.next()

	override fun remove() = this.iterator.remove()
}

class BTreeMultiMapKeys<K : Comparable<K>, V : Comparable<V>>(
	val tree: BTreeMultiMap<K, V>
) :
	MutableSet<K>
{
	override fun add(element: K): Boolean =
		throw UnsupportedOperationException()
	override fun addAll(elements: Collection<K>): Boolean =
		throw UnsupportedOperationException()

	override fun iterator(): MutableIterator<K> =
		MutableIteratorFacade(this.tree.iterator(), { it.key })

	override fun remove(element: K): Boolean = this.tree.remove(element).isNotEmpty()
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

class BTreeMultiMapValues<K : Comparable<K>, V : Comparable<V>>(
	val tree: BTreeMultiMap<K, V>
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
