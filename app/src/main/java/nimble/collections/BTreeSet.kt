// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.collections

open class BTreeSet<T>(
	val minSize: Int,
	val maxSize: Int,
	@Suppress("UNCHECKED_CAST")
	val compare: (T, T) -> Int = { a, b -> (a as Comparable<T>).compareTo(b) }
) :
	MutableSet<T>
{
	var root: BTreeNode<T> = BTreeLeafNode(this.minSize, this.maxSize)

	fun splitRoot()
	{
		val split = this.root.split()
		this.root = BTreeBranchNode<T>(this.minSize, this.maxSize)
			.also({
				it.nodes[0] = this.root
				it.nodes[1] = split.second
				it.data[0] = split.first
				it.dataSize = 1
			})
	}
	fun removeEmptyRoot(): Boolean
	{
		val root = this.root
		return if(root.dataSize == 0 && root is BTreeBranchNode<T>)
		{
			this.root = root.nodes[0]!!
			true
		}
		else
			false
	}

	override fun add(element: T): Boolean
	{
		val prev = this.root.add(
			element,
			this.compare)
		if(this.root.isFull)
			this.splitRoot()
		return prev != element
	}
	override fun addAll(elements: Collection<T>): Boolean
	{
		var changed = false
		for(x in elements)
			changed = this.add(x) || changed
		return changed
	}
	fun compactAddAll(iterable: Iterable<T>)
	{
		// Make sure root is a branch node, this is to avoid branching in main loop
		if(this.root !is BTreeBranchNode<T>)
			this.root = BTreeBranchNode<T>(this.minSize, this.maxSize).also({
				it.nodes[0] = this.root
			})

		val nodes = mutableListOf<BTreeBranchNode<T>>()
		var node = this.root
		while(node is BTreeBranchNode<T>)
		{
			nodes.add(0, node)
			node = node.lastNode()
		}
		var leafNode = node as BTreeLeafNode<T>

		val iter =  iterable.iterator()
		if(!iter.hasNext())
			return

		// Ensure there's a initial last value
		if(leafNode.dataSize == 0)
			leafNode.add(leafNode.dataSize, iter.next())

		@Suppress("UNCHECKED_CAST")
		var last = leafNode.data[leafNode.dataSize - 1] as T
		compactAdd@while(iter.hasNext())
		{
			val v = iter.next()
			val compare = this.compare(last, v)
			when
			{
				compare <= 0 ->
				{
					leafNode.add(leafNode.dataSize, v)
					last = v
				}
				compare == 0 ->
				{
					leafNode.data[leafNode.dataSize - 1] = v
					last = v
				}
				// Give up and add normally if data out of order
				else ->
				{
					this.add(v)
					break@compactAdd
				}
			}

			// Split nodes where necessary
			if(leafNode.isFull)
			{
				run()
				{
					val parentNode = nodes.first()
					val split = leafNode.compactSplitRight()
					parentNode.insertWithRightNode(parentNode.dataSize, split.first, split.second)
					leafNode = split.second as BTreeLeafNode<T>
				}

				for(i in 0 until nodes.size - 1)
				{
					val childNode = nodes[i]
					if(childNode.isFull)
					{
						val parentNode = nodes[i + 1]
						val split = childNode.compactSplitRight()
						parentNode.addLast(split.first, split.second)
						nodes[i] = split.second as BTreeBranchNode<T>
					}
					else
						continue
				}

				if(nodes.last().isFull)
				{
					val childNode = nodes.last()
					if(childNode.isFull)
					{
						val newNode = BTreeBranchNode<T>(minSize, maxSize)
						val split = childNode.compactSplitRight()
						newNode.nodes[0] = childNode
						newNode.addLast(split.first, split.second)
						nodes[nodes.size - 1] = split.second as BTreeBranchNode<T>
						nodes.add(newNode)
					}
				}
			}
		}

		// Remove root branch node if we didn't need it
		this.root = if(nodes.last().dataSize == 0)
			leafNode
		else
			nodes.last()

		// Add remaining values if we had to give up due to data being out of order
		if(iter.hasNext())
			this.addAll(iter.asSequence())
	}

	override fun iterator(): BTreeIterator<T> =
		BTreeIterator(this, mutableListOf()).inc()

	override fun remove(element: T): Boolean =
		(this.root.remove(element, this.compare) != null)
	override fun removeAll(elements: Collection<T>): Boolean
	{
		var removal = false
		for(e in elements)
			removal = this.remove(e) || removal
		return removal
	}
	override fun retainAll(elements: Collection<T>): Boolean =
		this.iterator().removeIf({ it !in elements })
	override fun clear()
	{
		this.root = BTreeLeafNode(this.minSize, this.maxSize)
	}

	override val size: Int get() = this.root.size
	override fun isEmpty(): Boolean = (this.size == 0)

	override fun contains(element: T): Boolean =
		(this.root.get(element, this.compare) != null)
	override fun containsAll(elements: Collection<T>): Boolean =
		elements.all({ it in this })

	override fun equals(other: Any?): Boolean
	{
		if(other === this)
			return true

		val other2 = other as? Set<*> ?: return false
		if(other2.size != this.size)
			return false
		return this.containsAll(other2)
	}
	override fun hashCode(): Int = this.iterator().asSequence().sumOf({ it.hashCode() })
}

data class BTreeNodeIterator<T>(
	val node: BTreeNode<T>,
	var index: Int
) :
	ListIterator<T>
{
	operator fun inc(): BTreeNodeIterator<T>
	{
		++this.index
		return this
	}
	operator fun dec(): BTreeNodeIterator<T>
	{
		--this.index
		return this
	}

	fun isLeaf() = this.node.isLeaf
	fun child() = BTreeNodeIterator(
		(this.node as BTreeBranchNode<T>).nodes[this.index]!!,
		0)
	fun previousChild() = (this.node as BTreeBranchNode<T>).nodes[this.index]!!
		.let({
			if(it is BTreeBranchNode<T>)
				BTreeNodeIterator(it, it.dataSize)
			else
				BTreeNodeIterator(it, it.dataSize - 1)
		})

	@Suppress("UNCHECKED_CAST")
	fun value() = this.node.data[this.index] as T

	override fun hasPrevious(): Boolean = this.index > 0
	override fun hasNext(): Boolean = this.index < this.node.dataSize

	override fun previousIndex(): Int = this.index - 1
	override fun nextIndex(): Int = this.index

	@Suppress("UNCHECKED_CAST")
	override fun previous(): T = this.node.data[--this.index] as T
	@Suppress("UNCHECKED_CAST")
	override fun next(): T = this.node.data[this.index++] as T
}

data class BTreeIterator<T>(
	val tree: BTreeSet<T>,
	val nodes: MutableList<BTreeNodeIterator<T>>
) :
	MutableIterator<T>
{
	operator fun dec(): BTreeIterator<T>
	{
		// If invalid iterator, move to last entry
		if(this.nodes.isEmpty())
		{
			if(this.tree.root.dataSize == 0)
				return this

			this.nodes.add(BTreeNodeIterator(
				this.tree.root,
				if(this.tree.root is BTreeBranchNode<T>)
					this.tree.root.dataSize
				else
					this.tree.root.dataSize - 1))
			while(!this.nodes.last().isLeaf())
				this.nodes.add(this.nodes.last().previousChild())

			return this
		}

		if(this.nodes.last().isLeaf())
		{
			this.nodes.last().dec()
			while(this.nodes.isNotEmpty() && this.nodes.last().index < 0)
			{
				this.nodes.removeAt(this.nodes.size - 1)
				this.nodes.lastOrNull()?.dec()
			}
		}
		else
			while(!this.nodes.last().isLeaf())
				this.nodes.add(this.nodes.last().previousChild())

		return this
	}
	operator fun inc(): BTreeIterator<T>
	{
		// If invalid iterator, move to first entry
		if(this.nodes.isEmpty())
		{
			if(this.tree.root.dataSize == 0)
				return this

			this.nodes.add(BTreeNodeIterator(this.tree.root, 0))
			while(!this.nodes.last().isLeaf())
				this.nodes.add(this.nodes.last().child())

			return this
		}

		this.nodes.last().inc()
		if(this.nodes.last().isLeaf())
			while(this.nodes.isNotEmpty() && !this.nodes.last().hasNext())
				this.nodes.removeAt(this.nodes.size - 1)
		else
			while(!this.nodes.last().isLeaf())
				this.nodes.add(this.nodes.last().child())

		return this
	}

	fun value() = this.nodes.last().value()

	override fun hasNext(): Boolean = this.nodes.isNotEmpty()

	override fun next(): T
	{
		val value = this.nodes.last().value()
		this.inc()
		return value
	}

	private fun rebalanceNodes()
	{
		for(i in this.nodes.size - 2 downTo 0)
		{
			val iter = this.nodes[i]
			val childIter = this.nodes[i + 1]
			if(childIter.node.isUndersized)
			{
				val node = iter.node as BTreeBranchNode<T>
				if(iter.index == node.dataSize)
				{
					if(node.nodes[iter.index - 1]!!.dataSize > node.minSize)
					{
						node.moveFromLeftNode(iter.index)
						++childIter.index
					}
					else
					{
						val leftNodeSize = node.nodes[iter.index - 1]!!.dataSize
						node.mergeChildren(iter.index - 1)
						--iter.index
						this.nodes[i + 1] = BTreeNodeIterator(
							node.nodes[iter.index]!!,
							leftNodeSize + 1 + childIter.index)
					}
				}
				else
				{
					if(node.nodes[iter.index + 1]!!.dataSize > node.minSize)
						node.moveFromRightNode(iter.index)
					else
						node.mergeChildren(iter.index)
				}
			}
			else
				break
		}
	}

	override fun remove()
	{
		this.dec()

		val last = this.nodes.last()
		when(last.node)
		{
			is BTreeLeafNode<T> ->
			{
				last.node.remove(last.index)
				if(last.index == last.node.dataSize)
				{
					--last.index
					this.rebalanceNodes()
					this.inc()
				}
			}
			is BTreeBranchNode<T> ->
			{
				val takeNode = last.node.nodes[last.index + 1]!!
				last.node.data[last.index] = takeNode.takeLeftMostEntry()

				if(takeNode.isUndersized)
				{
					if(last.node.nodes[last.index]!!.dataSize > last.node.minSize)
					{
						last.node.moveFromLeftNode(last.index + 1)
						this.rebalanceNodes()
						@Suppress("NAME_SHADOWING")
						val last = this.nodes.last()
						++last.index
						this.nodes.add(last.child())
					}
					else
					{
						val leftNodeSize = last.node.nodes[last.index]!!.dataSize
						last.node.mergeChildren(last.index)
						this.nodes.add(BTreeNodeIterator(
							last.node.nodes[last.index]!!,
							leftNodeSize))
						this.rebalanceNodes()
					}
				}
			}
		}

		if(this.tree.removeEmptyRoot())
			this.nodes.removeAt(0)
	}
}

