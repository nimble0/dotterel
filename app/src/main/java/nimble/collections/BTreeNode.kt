// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

@file:Suppress("UNCHECKED_CAST")

package nimble.collections

interface BTreeNode<T>
{
	val minSize: Int
	var dataSize: Int
	var data: Array<Any?>

	val isLeaf: Boolean
	val isUndersized: Boolean
	val isFull: Boolean
	val size: Int

	fun merge(v: T, node: BTreeNode<T>)
	fun takeLeftMostEntry(): T
	fun takeRightMostEntry(): T
	fun takeFromLeftNode(v: T, node: BTreeNode<T>)
	fun takeFromRightNode(v: T, node: BTreeNode<T>)

	fun split(i: Int): Pair<T, BTreeNode<T>>
	fun split(): Pair<T, BTreeNode<T>> = this.split((this.dataSize - 1) / 2)
	fun compactSplitLeft(): Pair<T, BTreeNode<T>> = this.split(1)
	fun compactSplitRight(): Pair<T, BTreeNode<T>> = this.split(this.dataSize - 2)

	fun removeFirst()
	fun removeLast()

	fun <T2> binarySearch(v: T2, compare: ((T2, T) -> Int)): Int
	{
		var low = 0
		var high = this.dataSize
		while(true)
		{
			if(high <= low)
				return low

			val mid = (low + high) / 2
			val comparison = compare(v, this.data[mid] as T)
			when
			{
				comparison < 0 -> high = mid
				comparison == 0 -> return mid
				comparison > 0 -> low = mid + 1
			}
		}
	}

	fun <T2> get(v: T2, compare: ((T2, T) -> Int)): T?
	fun <T2> find(v: T2, compare: ((T2, T) -> Int), iterator: BTreeIterator<T>): BTreeIterator<T>

	fun add(v: T, compare: ((T, T) -> Int)): T?
	fun addIfAbsent(v: T, compare: ((T, T) -> Int)): T?

	fun <T2> remove(v: T2, compare: ((T2, T) -> Int)): T?
}

class BTreeLeafNode<T>(
	override val minSize: Int,
	maxSize: Int
) :
	BTreeNode<T>
{
	override var dataSize: Int = 0
	override var data: Array<Any?> = Array(maxSize, { null })

	override val isLeaf: Boolean get() = true
	override val isUndersized: Boolean
		get() = this.dataSize < this.minSize
	override val isFull: Boolean
		get() = this.dataSize >= this.data.size
	override val size: Int
		get() = this.dataSize

	fun newNode(minSize: Int, maxSize: Int) =
		BTreeLeafNode<T>(minSize, maxSize)

	override fun merge(v: T, node: BTreeNode<T>)
	{
		this.data[this.dataSize++] = v
		node.data.copyInto(this.data, this.dataSize, 0, node.dataSize)
		this.dataSize += node.dataSize
	}

	override fun takeLeftMostEntry(): T
	{
		val v = this.data[0] as T
		this.remove(0)
		return v
	}
	override fun takeRightMostEntry(): T
	{
		val v = this.data[this.dataSize - 1] as T
		this.data[--this.dataSize] = null
		return v
	}

	override fun takeFromLeftNode(v: T, node: BTreeNode<T>) =
		this.add(0, v)
	override fun takeFromRightNode(v: T, node: BTreeNode<T>)
	{
		this.data[this.dataSize++] = v
	}

	override fun split(i: Int): Pair<T, BTreeNode<T>>
	{
		val splitV = this.data[i]
		val newNode = this.newNode(this.minSize, this.data.size)
		this.data.copyInto(newNode.data, 0, i + 1, this.dataSize)
		this.data.fill(null, i, this.dataSize)
		newNode.dataSize = this.dataSize - i - 1
		this.dataSize = i

		return Pair(splitV as T, newNode)
	}

	fun add(i: Int, v: T)
	{
		this.data.copyInto(this.data, i + 1, i, this.dataSize)
		this.data[i] = v
		++this.dataSize
	}

	fun remove(i: Int)
	{
		this.data.copyInto(this.data, i, i + 1, this.dataSize)
		this.data[--this.dataSize] = null
	}

	override fun removeFirst() = this.remove(0)
	override fun removeLast() { this.data[--this.dataSize] = null }


	override fun <T2> get(v: T2, compare: (T2, T) -> Int): T?
	{
		val i = this.binarySearch(v, compare)
		return if(i < this.dataSize && compare(v, this.data[i] as T) == 0)
			this.data[i] as T
		else
			null
	}
	override fun <T2> find(
		v: T2,
		compare: (T2, T) -> Int,
		iterator: BTreeIterator<T>
	): BTreeIterator<T>
	{
		val i = this.binarySearch(v, compare)
		if(i == this.dataSize)
		{
			iterator.nodes.add(BTreeNodeIterator(this, i - 1))
			iterator.inc()
		}
		else
			iterator.nodes.add(BTreeNodeIterator(this, i))
		return iterator
	}

	override fun add(v: T, compare: (T, T) -> Int): T?
	{
		val i = this.binarySearch(v, compare)
		return if(i < this.dataSize && compare(v, this.data[i] as T) == 0)
		{
			val prev = this.data[i] as T
			this.data[i] = v
			return prev
		}
		else
		{
			this.add(i, v)
			null
		}
	}
	override fun addIfAbsent(v: T, compare: (T, T) -> Int): T?
	{
		val i = this.binarySearch(v, compare)
		return if(i < this.dataSize && compare(v, this.data[i] as T) == 0)
			this.data[i] as T
		else
		{
			this.add(i, v)
			null
		}
	}

	override fun <T2> remove(v: T2, compare: (T2, T) -> Int): T?
	{
		val i = this.binarySearch(v, compare)
		return if(i < this.dataSize && compare(v, this.data[i] as T) == 0)
		{
			val prev = this.data[i] as T
			this.remove(i)
			prev
		}
		else
			null
	}
}

class BTreeBranchNode<T>(
	override val minSize: Int,
	maxSize: Int
) :
	BTreeNode<T>
{
	override var dataSize: Int = 0
	override var data: Array<Any?> = Array(maxSize, { null })
	var nodes = Array<BTreeNode<T>?>(maxSize + 1, { null })

	override val isLeaf: Boolean get() = false
	override val isUndersized: Boolean
		get() = this.dataSize < this.minSize
	override val isFull: Boolean
		get() = this.dataSize >= this.data.size
	override val size: Int
		get() = (this.dataSize
			+ this.nodes
			.asSequence()
			.take(this.dataSize + 1)
			.sumBy({ it!!.size }))

	fun newNode(minSize: Int, maxSize: Int) =
		BTreeBranchNode<T>(minSize, maxSize)

	fun firstNode() = this.nodes[0]!!
	fun lastNode() = this.nodes[this.dataSize]!!

	override fun merge(v: T, node: BTreeNode<T>)
	{
		@Suppress("NAME_SHADOWING")
		val node = node as BTreeBranchNode<T>

		this.data[this.dataSize++] = v
		node.data.copyInto(this.data, this.dataSize, 0, node.dataSize)
		node.nodes.copyInto(this.nodes, this.dataSize, 0, node.dataSize + 1)
		this.dataSize += node.dataSize
	}

	override fun takeLeftMostEntry(): T
	{
		val takeNode = this.nodes[0]!!
		val v = takeNode.takeLeftMostEntry()
		if(takeNode.isUndersized)
			this.rebalance(0)
		return v
	}
	override fun takeRightMostEntry(): T
	{
		val takeNode = this.nodes[this.dataSize]!!
		val v = takeNode.takeRightMostEntry()
		if(takeNode.isUndersized)
			this.rebalance(this.dataSize)
		return v
	}

	override fun takeFromLeftNode(v: T, node: BTreeNode<T>) =
		this.addFirst(v, (node as BTreeBranchNode<T>).lastNode())
	override fun takeFromRightNode(v: T, node: BTreeNode<T>) =
		this.addLast(v, (node as BTreeBranchNode<T>).firstNode())

	fun moveFromLeftNode(i: Int)
	{
		val takeFromNode = this.nodes[i - 1]!!
		this.nodes[i]!!.takeFromLeftNode(this.data[i - 1] as T, takeFromNode)
		this.data[i - 1] = takeFromNode.data[takeFromNode.dataSize - 1]
		takeFromNode.removeLast()
	}
	fun moveFromRightNode(i: Int)
	{
		val takeFromNode = this.nodes[i + 1]!!
		this.nodes[i]!!.takeFromRightNode(this.data[i] as T, takeFromNode)
		this.data[i] = takeFromNode.data[0]
		takeFromNode.removeFirst()
	}

	override fun split(i: Int): Pair<T, BTreeNode<T>>
	{
		val splitV = this.data[i]
		val newNode = this.newNode(this.minSize, this.data.size)
		this.data.copyInto(newNode.data, 0, i + 1, this.dataSize)
		this.data.fill(null, i, this.dataSize)
		this.nodes.copyInto(newNode.nodes, 0, i + 1, this.dataSize + 1)
		this.nodes.fill(null, i + 1, this.dataSize + 1)
		newNode.dataSize = this.dataSize - i - 1
		this.dataSize = i

		return Pair(splitV as T, newNode)
	}

	fun insertWithLeftNode(i: Int, v: T, node: BTreeNode<T>)
	{
		this.data.copyInto(this.data, i + 1, i, this.dataSize)
		this.data[i] = v
		this.nodes.copyInto(this.nodes, i + 1, i, this.dataSize + 1)
		this.nodes[i] = node
		++this.dataSize
	}
	fun insertWithRightNode(i: Int, v: T, node: BTreeNode<T>)
	{
		this.data.copyInto(this.data, i + 1, i, this.dataSize)
		this.data[i] = v
		this.nodes.copyInto(this.nodes, i + 2, i + 1, this.dataSize + 1)
		this.nodes[i + 1] = node
		++this.dataSize
	}
	fun addFirst(v: T, node: BTreeNode<T>) = this.insertWithLeftNode(0, v, node)
	fun addLast(v: T, node: BTreeNode<T>)
	{
		this.data[this.dataSize++] = v
		this.nodes[this.dataSize] = node
	}

	fun remove(i: Int): T
	{
		val removeNode = this.nodes[i] as BTreeNode<T>
		val prevValue = this.data[i] as T
		this.data[i] = removeNode.takeRightMostEntry()
		if(removeNode.isUndersized)
			this.rebalance(i)
		return prevValue
	}

	fun removeWithLeftNode(i: Int)
	{
		this.data.copyInto(this.data, i, i + 1, this.dataSize)
		this.data[this.dataSize - 1] = null
		this.nodes.copyInto(this.nodes, i, i + 1, this.dataSize + 1)
		this.nodes[this.dataSize] = null
		--this.dataSize
	}
	fun removeWithRightNode(i: Int)
	{
		this.data.copyInto(this.data, i, i + 1, this.dataSize)
		this.data[this.dataSize - 1] = null
		this.nodes.copyInto(this.nodes, i + 1, i + 2, this.dataSize + 1)
		this.nodes[this.dataSize] = null
		--this.dataSize
	}

	override fun removeFirst() = this.removeWithLeftNode(0)
	override fun removeLast()
	{
		this.data[this.dataSize - 1] = null
		this.nodes[this.dataSize] = null
		--this.dataSize
	}

	fun splitChild(i: Int)
	{
		val split = this.nodes[i]!!.split()
		this.insertWithRightNode(i, split.first, split.second)
	}

	fun mergeChildren(i: Int)
	{
		this.nodes[i]!!.merge(
			this.data[i] as T,
			this.nodes[i + 1]!!)

		this.removeWithRightNode(i)
	}

	fun rebalance(i: Int)
	{
		if(this.dataSize == 0)
			return

		if(i == this.dataSize)
		{
			if(this.nodes[i - 1]!!.dataSize > this.minSize)
				this.moveFromLeftNode(i)
			else
				this.mergeChildren(i - 1)
		}
		else
		{
			if(this.nodes[i + 1]!!.dataSize > this.minSize)
				this.moveFromRightNode(i)
			else
				this.mergeChildren(i)
		}
	}

	override fun <T2> get(v: T2, compare: (T2, T) -> Int): T?
	{
		val i = this.binarySearch(v, compare)
		val dataValue = this.data[i]
		return if(i < this.dataSize && compare(v, dataValue as T) == 0)
			dataValue
		else
			(this.nodes[i] as BTreeNode<T>).get(v, compare)
	}
	override fun <T2> find(
		v: T2,
		compare: (T2, T) -> Int,
		iterator: BTreeIterator<T>
	): BTreeIterator<T>
	{
		val i = this.binarySearch(v, compare)
		iterator.nodes.add(BTreeNodeIterator(this, i))
		return if(i < this.dataSize && compare(v, this.data[i] as T) == 0)
			iterator
		else
			(this.nodes[i] as BTreeNode<T>).find(v, compare, iterator)
	}

	override fun add(v: T, compare: (T, T) -> Int): T?
	{
		val i = this.binarySearch(v, compare)
		val dataValue = this.data[i]
		return if(i < this.dataSize && compare(v, dataValue as T) == 0)
		{
			this.data[i] = v
			dataValue
		}
		else
		{
			val node = this.nodes[i] as BTreeNode<T>
			val prevValue = node.add(v, compare)
			if(node.isFull)
				this.splitChild(i)
			return prevValue
		}
	}
	override fun addIfAbsent(v: T, compare: (T, T) -> Int): T?
	{
		val i = this.binarySearch(v, compare)
		val dataValue = this.data[i]
		return if(i < this.dataSize && compare(v, dataValue as T) == 0)
			dataValue
		else
		{
			val node = this.nodes[i] as BTreeNode<T>
			val prevValue = node.addIfAbsent(v, compare)
			if(node.isFull)
				this.splitChild(i)
			return prevValue
		}
	}

	override fun <T2> remove(v: T2, compare: ((T2, T) -> Int)): T?
	{
		val i = this.binarySearch(v, compare)
		val node = this.nodes[i] as BTreeNode<T>
		return if(i < this.dataSize && compare(v, this.data[i] as T) == 0)
		{
			val prevValue = this.data[i] as T
			this.data[i] = node.takeRightMostEntry()
			if(node.isUndersized)
				this.rebalance(i)
			prevValue
		}
		else
		{
			val prevValue = node.remove(v, compare)
			if(node.isUndersized)
				this.rebalance(i)
			prevValue
		}
	}
}
