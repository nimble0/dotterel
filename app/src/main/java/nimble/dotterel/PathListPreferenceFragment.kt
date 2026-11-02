// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AbsListView.*

import com.eclipsesource.json.*

import nimble.dotterel.util.toJson
import nimble.dotterel.util.ui.ReorderableListAdapter
import nimble.dotterel.util.ui.ReorderableListView

data class PathItem(
	var path: String,
	var enabled: Boolean,
	var accessible: Boolean = true)

private class PathListPreferenceAdapter(
	context: Context,
	val items: MutableList<PathItem> = mutableListOf()
) :
	ArrayAdapter<PathItem>(context, R.layout.pref_path_list_item, items),
	ReorderableListAdapter
{
	override fun removeAt(i: Int)
	{
		this.items.removeAt(i)
		this.notifyDataSetChanged()
	}
	override fun move(from: Int, to: Int)
	{
		val move = this.items.removeAt(from)
		this.items.add(to, move)
		this.notifyDataSetChanged()
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup)
		: View
	{
		val v = convertView ?: LayoutInflater.from(this.context)
			.inflate(R.layout.pref_path_list_item, parent, false)

		val item = this.getItem(position) ?: return v

		v.findViewById<TextView>(R.id.path)
			.also({
				it.text = item.path
				it.isEnabled = item.enabled
			})

		val background = if(item.accessible)
			R.drawable.path_item
		else
			R.drawable.path_item_inaccessible
		v.background =
			if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
				this.context.resources.getDrawable(background, null)
			else
				@Suppress("DEPRECATION")
				this.context.resources.getDrawable(background)

		return v
	}
}

private class PathListPreferenceModalListener(
	val list: ReorderableListView,
	val adapter: PathListPreferenceAdapter
) :
	MultiChoiceModeListener
{
	override fun onItemCheckedStateChanged(
		mode: ActionMode,
		i: Int,
		id: Long,
		checked: Boolean)
	{
		mode.title = "${this.list.checkedItemCount} Selected"
	}

	override fun onActionItemClicked(mode: ActionMode, menu: MenuItem): Boolean
	{
		val selected = this.list.checkedItemPositions
		return when(menu.itemId)
		{
			R.id.list_enable ->
			{
				for((i, item) in this.adapter.items.withIndex())
					if(selected[i])
						item.enabled = true
				this.adapter.notifyDataSetChanged()

				mode.finish()
				true
			}
			R.id.list_disable ->
			{
				for((i, item) in this.adapter.items.withIndex())
					if(selected[i])
						item.enabled = false
				this.adapter.notifyDataSetChanged()

				mode.finish()
				true
			}
			R.id.list_delete ->
			{
				val newItems = this.adapter.items
					.filterIndexed({ i, _ -> !selected[i] })
				this.adapter.items.clear()
				this.adapter.items.addAll(newItems)
				this.adapter.notifyDataSetChanged()

				mode.finish()
				true
			}
			else -> false
		}
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean
	{
		mode.menuInflater.inflate(R.menu.path_list_context, menu)
		this.list.allowDragging = false
		this.list.actionMode = mode
		return true
	}

	override fun onDestroyActionMode(mode: ActionMode)
	{
		this.list.allowDragging = true
		this.list.actionMode = null
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
}


abstract class PathListPreferenceFragment : PreferenceFragment()
{
	protected var readOnly = true
	protected var view: ReorderableListView? = null
	private var adapter: PathListPreferenceAdapter? = null
	protected val systemManager get() =
		(this.requireActivity().application as DotterelApplication)
		.systemManager

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.setHasOptionsMenu(true)

		this.readOnly = this.arguments?.getBoolean("readOnly") ?: false
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View
	{
		if(this.preference == null)
			this.preference = this.loadPreference()

		val view = inflater.inflate(R.layout.pref_dictionaries, container, false)

		val adapter = PathListPreferenceAdapter(inflater.context)

		val listView = view.findViewById<ReorderableListView>(R.id.dictionaries)
		listView.adapter = adapter
		if(!this.readOnly)
		{
			listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
			listView.setMultiChoiceModeListener(
				PathListPreferenceModalListener(listView, adapter))
		}
		listView.allowDragging = !this.readOnly

		this.adapter = adapter
		this.view = listView

		return view
	}

	override fun onDestroyView()
	{
		this.view?.actionMode?.finish()
		super.onDestroyView()
	}

	override fun onResume()
	{
		super.onResume()
		this.load()
	}

	override fun onPause()
	{
		this.save()
		super.onPause()
	}

	fun reset()
	{
		if(this.readOnly)
			return

		val preference = this.preference!!
		(preference.preferenceDataStore as? JsonDataStore)
			?.safePut(preference.key, null)

		this.load()
	}

	fun add(uri: String)
	{
		this.adapter?.also({
			if(it.items.all({ dictionary -> dictionary.path != uri }))
			{
				it.items.add(PathItem(uri, true))
				it.notifyDataSetChanged()
			}
		})
	}

	fun save()
	{
		if(this.readOnly)
			return

		val dictionaries = this.adapter?.items?.map({ item ->
				JsonObject().also({
					it.add("path", item.path)
					it.add("enabled", item.enabled)
				})
			})?.toJson()
			?: return

		val preference = this.preference!!
		(preference.preferenceDataStore as? JsonDataStore)
			?.safePut(preference.key, dictionaries)
	}

	protected abstract fun isAccessible(path: String): Boolean

	open fun load()
	{
		val preference = this.preference!!
		val dataStore = (preference.preferenceDataStore as? JsonDataStore)
		val dictionaries = dataStore
			?.safeGet(
				preference.key,
				null,
				{ v ->
					v.asArray()
						.map({ it.asObject() })
						.map({
							val path = it.get("path").asString()
							PathItem(
								path,
								it.get("enabled").asBoolean(),
								this.isAccessible(path))
						})
				})
			?: listOf()

		this.adapter?.clear()
		this.adapter?.addAll(dictionaries)
		this.adapter?.notifyDataSetChanged()
	}
}
