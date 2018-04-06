// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.annotation.SuppressLint
import android.content.Context
import android.database.DataSetObserver
import android.util.AttributeSet
import android.view.*
import android.widget.*

import nimble.dotterel.util.*

class Grabber(context: Context, attributes: AttributeSet)
	: TextView(context, attributes)
{
	var listView: ReorderableListView? = null
	var index: Int = -1

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(e: MotionEvent): Boolean
	{
		if(e.actionMasked == MotionEvent.ACTION_DOWN)
			this.listView?.startDrag(this.index)

		return false
	}
}

interface ReorderableListAdapter : ListAdapter
{
	fun removeAt(i: Int)
	fun move(from: Int, to: Int)
}

private class BridgeAdapter(
	private val listView: ReorderableListView,
	val adapter: ReorderableListAdapter
) :
	BaseAdapter()
{
	override fun getCount() = this.adapter.count

	override fun getItemId(i: Int) = this.adapter.getItemId(i)
	override fun getItem(i: Int): Any = this.adapter.getItem(i)

	override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View
	{
		val v = this.adapter.getView(i, convertView, parent)
		val grabber = v.findViewById<Grabber>(R.id.grabber)
		grabber.text = i.toString()
		grabber.index = i
		grabber.listView = this.listView

		return v
	}

	override fun registerDataSetObserver(observer: DataSetObserver?)
	{
		super.registerDataSetObserver(observer)
		this.adapter.registerDataSetObserver(observer)
	}
	override fun unregisterDataSetObserver(observer: DataSetObserver?)
	{
		super.unregisterDataSetObserver(observer)
		this.adapter.unregisterDataSetObserver(observer)
	}
}

class ReorderableListView(context: Context, attributes: AttributeSet)
	: ListView(context, attributes)
{
	private var _adapter: BridgeAdapter? = null
	var adapter: ReorderableListAdapter?
		get() = this._adapter?.adapter
		set(v)
		{
			if(v != null)
			{
				this._adapter = BridgeAdapter(this, v)
				super.setAdapter(this._adapter)
			}
		}
	var actionMode: ActionMode? = null

	var allowDragging = true
	private var dragI: Int? = null

	fun startDrag(i: Int): Boolean
	{
		if(!allowDragging)
			return false

		this.dragI = i
		this.getChildAt(i - this.firstVisiblePosition)?.isSelected = true
		return true
	}

	private fun updateDrag(p: Vector2)
	{
		val from = this.dragI
		if(from != null)
		{
			val to = this.pointToPosition(p.x.toInt(), p.y.toInt())
			if(to != AdapterView.INVALID_POSITION)
			{
				val toView = this.getChildAt(to - this.firstVisiblePosition) ?: return

				val middleToView = toView.position.y - this.position.y + toView.size.y / 2

				if((to < from && p.y < middleToView)
					|| (to > from && p.y > middleToView))
				{
					this.adapter?.move(from, to)
					this.dragI = to
					post({
						this.getChildAt(to - this.firstVisiblePosition)?.isSelected = true
					})
				}
			}
		}
	}

	private fun stopDrag()
	{
		val dragI = this.dragI
		if(dragI != null)
		{
			val v = this.getChildAt(dragI - this.firstVisiblePosition)
			v?.isSelected = false
			v?.performClick()
		}
		this.dragI = null
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(e: MotionEvent): Boolean
	{
		if(this.dragI != null)
		{
			when(e.actionMasked)
			{
				MotionEvent.ACTION_MOVE -> this.updateDrag(Vector2(e.x, e.y))
				MotionEvent.ACTION_CANCEL,
				MotionEvent.ACTION_UP -> this.stopDrag()
			}
			return true
		}

		return super.onTouchEvent(e)
	}
}
