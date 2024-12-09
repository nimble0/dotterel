// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

@file:Suppress("UNCHECKED_CAST")

package nimble.dotterel.util

import nimble.collections.facades.MutableIteratorFacade
import nimble.collections.removeIf

class BufferedMap<K, V>(
	val map: MutableMap<K, V> = mutableMapOf()
) :
	MutableMap<K, V>
{
	var bufferedMap: MutableMap<K, Any?>? = null

	internal class RemoveEntry

	override fun containsKey(key: K): Boolean
	{
		val bufferedMap = this.bufferedMap
		return if(bufferedMap != null)
			{
				val v = bufferedMap[key]
				when
				{
					v is RemoveEntry -> false
					v != null -> true
					else -> this.map.containsKey(key)
				}
			}
			else
				this.map.containsKey(key)
	}
	override fun containsValue(value: V): Boolean
	{
		val bufferedMap = this.bufferedMap
		if(bufferedMap != null)
		{
			if(bufferedMap.containsValue(value))
				return true
			else
			{
				for(kv in this.map)
					if(kv.value == value && bufferedMap[kv.key] !is RemoveEntry)
						return true
				return false
			}
		}
		else
			return this.map.containsValue(value)
	}
	override fun get(key: K): V?
	{
		val v = this.bufferedMap?.get(key)
		return if(v is RemoveEntry)
				null
			else
				v as V? ?: this.map[key]
	}

	override fun put(key: K, value: V): V?
	{
		val bufferedMap = this.bufferedMap
		return if(bufferedMap != null)
		{
			val v = bufferedMap[key]
			bufferedMap[key] = value
			if(v is RemoveEntry)
				null
			else
				v as V? ?: this.map[key]
		}
		else
			this.map.put(key, value)
	}
	override fun putAll(from: Map<out K, V>)
	{
		val bufferedMap = this.bufferedMap
		if(bufferedMap != null)
			bufferedMap.putAll(from)
		else
			this.map.putAll(from)
	}

	override fun remove(key: K): V?
	{
		val bufferedMap = this.bufferedMap
		return if(bufferedMap != null)
		{
			val v = bufferedMap[key]
			if(this.map.containsKey(key))
				bufferedMap[key] = RemoveEntry()
			else
				bufferedMap.remove(key)
			if(v is RemoveEntry)
				null
			else
				v as V? ?: this.map[key]
		}
		else
			this.map.remove(key)
	}
	override fun clear()
	{
		val bufferedMap = this.bufferedMap
		return if(bufferedMap != null)
		{
			bufferedMap.clear()
			for(kv in this.map)
				bufferedMap[kv.key] = RemoveEntry()
		}
		else
			map.clear()
	}

	override val size: Int
		get()
		{
			val bufferedMap = this.bufferedMap
			return if(bufferedMap != null)
			{
				this.map.size + bufferedMap.asSequence().sumBy({
						if(this.map.containsKey(it.key))
						{
							if(it.value is RemoveEntry)
								-1
							else
								0
						}
						else
							1
					})
			}
			else
				this.map.size
		}
	override fun isEmpty(): Boolean = (this.size == 0)

	override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
		get() = BufferedMapEntries(this)
	override val keys: MutableSet<K>
		get() = BufferedMapKeys(this)
	override val values: MutableCollection<V>
		get() = BufferedMapValues(this)

	fun startBuffering()
	{
		this.bufferedMap = mutableMapOf()
	}
	fun flush()
	{
		val bufferedMap = this.bufferedMap ?: return
		for(e in bufferedMap)
		{
			val v = e.value
			if(v is RemoveEntry)
				this.map.remove(e.key)
			else
				this.map[e.key] = v as V
		}
		this.bufferedMap = null
	}

	override fun equals(other: Any?): Boolean
	{
		if(other === this)
			return true

		val other2 = other as? Map<*, *> ?: return false
		if(other2.size != size)
			return false
		for(x in this)
			if(other2[x.key] != x.value)
				return false
		return true
	}
	override fun hashCode(): Int = this.entries.sumOf({ it.hashCode() })
}

class BufferedMapIterator<K, V>(
	map: BufferedMap<K, V>
) :
	MutableIterator<MutableMap.MutableEntry<K, V>>
{
	private val map = map.map
	private val bufferedMap = map.bufferedMap!!
	private val iterator = this.map.iterator()
	private val bufferedIterator = this.bufferedMap.iterator()
	private var iteratingMain = true
	private var value: MutableMap.MutableEntry<K, V>? = null

	init
	{
		this.inc()
	}

	fun inc()
	{
		if(this.iteratingMain)
			while(true)
			{
				if(!this.iterator.hasNext())
				{
					this.iteratingMain = false
					return
				}

				val nextValue = this.iterator.next()
				if(!this.bufferedMap.containsKey(nextValue.key))
				{
					this.value = nextValue
					return
				}
			}
		else
			while(true)
			{
				if(!this.bufferedIterator.hasNext())
				{
					this.value = null
					return
				}

				val nextValue = this.bufferedIterator.next()
				if(nextValue.value !is BufferedMap.RemoveEntry)
				{
					this.value = nextValue as MutableMap.MutableEntry<K, V>
					return
				}
			}
	}

	override fun hasNext(): Boolean = (this.value != null)
	override fun next(): MutableMap.MutableEntry<K, V>
	{
		val v = this.value!!
		this.inc()
		return v
	}

	override fun remove() =
		throw UnsupportedOperationException("Bad Java iterator interface makes this impossible")
}

class BufferedMapEntries<K, V>(val map: BufferedMap<K, V>) : MutableSet<MutableMap.MutableEntry<K, V>>
{
	override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
		this.map.put(element.key, element.value) != element.value
	override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean
	{
		var changed = false
		for(x in elements)
			changed = this.add(x) || changed
		return changed
	}

	override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean =
		this.map[element.key] == element.value
	override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean =
		elements.all({ this.map[it.key] == it.value })

	override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> =
		if(this.map.bufferedMap == null)
			this.map.map.iterator()
		else
			BufferedMapIterator(this.map)

	override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean =
		if(this.map[element.key] == element.value)
		{
			this.map.remove(element.key)
			true
		}
		else
			false
	override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean
	{
		var removal = false
		for(e in elements)
			removal = this.remove(e) || removal
		return removal
	}
	override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean =
		this.iterator().removeIf({ it !in elements })
	override fun clear() = this.map.clear()

	override val size: Int get() = this.map.size
	override fun isEmpty(): Boolean = this.map.isEmpty()
}

class BufferedMapKeys<K, V>(val map: BufferedMap<K, V>) : MutableSet<K>
{
	override fun add(element: K): Boolean =
		throw UnsupportedOperationException()
	override fun addAll(elements: Collection<K>): Boolean =
		throw UnsupportedOperationException()

	override fun contains(element: K): Boolean = this.map.containsKey(element)
	override fun containsAll(elements: Collection<K>): Boolean =
		elements.all({ this.map.containsKey(it) })

	override fun iterator(): MutableIterator<K> =
		MutableIteratorFacade(this.map.iterator(), { it.key })

	override fun remove(element: K): Boolean =
		(this.map.remove(element) != null)
	override fun removeAll(elements: Collection<K>): Boolean
	{
		var removal = false
		for(e in elements)
			removal = this.remove(e) || removal
		return removal
	}
	override fun retainAll(elements: Collection<K>): Boolean
	{
		val removeEntries = this.iterator().asSequence()
			.filter({ it !in elements })
			.toList()
		for(e in removeEntries)
			this.remove(e)
		return removeEntries.isNotEmpty()
	}
	override fun clear() = this.map.clear()

	override val size: Int get() = this.map.size
	override fun isEmpty(): Boolean = this.map.isEmpty()
}

class BufferedMapValues<K, V>(val map: BufferedMap<K, V>) : MutableCollection<V>
{
	override fun add(element: V): Boolean =
		throw UnsupportedOperationException()
	override fun addAll(elements: Collection<V>): Boolean =
		throw UnsupportedOperationException()

	override fun contains(element: V): Boolean = this.map.containsValue(element)
	override fun containsAll(elements: Collection<V>): Boolean =
		elements.all({ this.map.containsValue(it) })

	override fun iterator(): MutableIterator<V> =
		MutableIteratorFacade(this.map.iterator(), { it.value })

	override fun remove(element: V): Boolean
	{
		val removeEntries = this.map.iterator().asSequence()
			.filter({ it.value == element })
			.map({ it.key })
			.toList()
		for(e in removeEntries)
			this.map.remove(e)
		return removeEntries.isNotEmpty()
	}
	override fun removeAll(elements: Collection<V>): Boolean
	{
		val removeEntries = this.map.iterator().asSequence()
			.filter({ it.value in elements })
			.map({ it.key })
			.toList()
		for(e in removeEntries)
			this.map.remove(e)
		return removeEntries.isNotEmpty()
	}
	override fun retainAll(elements: Collection<V>): Boolean
	{
		val removeEntries = this.map.iterator().asSequence()
			.filter({ it.value !in elements })
			.map({ it.key })
			.toList()
		for(e in removeEntries)
			this.map.remove(e)
		return removeEntries.isNotEmpty()
	}
	override fun clear() = this.map.clear()

	override val size: Int get() = this.map.size
	override fun isEmpty(): Boolean = this.map.isEmpty()
}
