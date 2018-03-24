// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.*
import android.util.*
import android.view.*
import android.widget.*
import android.widget.AbsListView.*

import com.beust.klaxon.*

import nimble.dotterel.translation.systems.IRELAND_SYSTEM

data class DictionaryItem(var name: String, var enabled: Boolean)

fun List<DictionaryItem>.toJson(): String
{
	val values = this
	return json({
		array(values.map({
			obj(
				"name" to it.name,
				"enabled" to it.enabled
			)
		}))
	}).toJsonString()
}

fun dictionaryListFromJson(key: String, json: String): List<DictionaryItem>
{
	try
	{
		return Klaxon().parseArray(json) ?: listOf()
	}
	catch(e: KlaxonException)
	{
		Log.e("Preferences", "Preference $key has badly formed JSON")
	}
	catch(e: IllegalStateException)
	{
		Log.e("Preferences", "Invalid type found while reading preference $key")
	}

	return listOf()
}

private class DictionariesPreferenceAdapter(
	context: Context,
	val items: MutableList<DictionaryItem> = mutableListOf()
) :
	ArrayAdapter<DictionaryItem>(context, R.layout.pref_dictionaries_item, items),
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
			.inflate(R.layout.pref_dictionaries_item, parent, false)

		val item = this.getItem(position)

		v.findViewById<TextView>(R.id.path)
			.apply({
				this.text = item.name
				this.isEnabled = item.enabled
			})

		return v
	}
}

private class DictionariesPreferenceModalListener(
	val list: ReorderableListView,
	val adapter: DictionariesPreferenceAdapter
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
		mode.menuInflater.inflate(R.menu.dictionaries_preference_menu, menu)
		this.list.allowDragging = false
		return true
	}

	override fun onDestroyActionMode(mode: ActionMode)
	{
		this.list.allowDragging = true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
}

const val SELECT_DICTIONARY_FILE = 2

class DictionariesPreferenceFragment : PreferenceFragment()
{
	private var key = "system/Ireland/dictionaries"
	private var view: ReorderableListView? = null
	private var adapter: DictionariesPreferenceAdapter? = null

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		this.key = "system/Ireland/dictionaries"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup,
		savedInstanceState: Bundle?
	): View
	{
		val view = inflater.inflate(R.layout.pref_dictionaries, container, false)

		val adapter = DictionariesPreferenceAdapter(inflater.context)

		val listView = (view.findViewById(R.id.dictionaries) as ReorderableListView)
		listView.adapter = adapter
		listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
		listView.setMultiChoiceModeListener(
			DictionariesPreferenceModalListener(listView, adapter))

		this.adapter = adapter
		this.view = listView

		view.findViewById<Button>(R.id.add_dictionary).setOnClickListener({
			this.chooseDictionaryFile()
		})

		view.findViewById<Button>(R.id.reset_dictionaries).setOnClickListener({
			this.reset()
		})

		return view
	}

	override fun onResume()
	{
		super.onResume()

		val value = PreferenceManager.getDefaultSharedPreferences(this.activity)
			.getString(this.key, null)

		if(value != null)
			this.load(value)
		else
			this.reset()
	}

	override fun onPause()
	{
		this.save()
		super.onPause()
	}

	private fun chooseDictionaryFile()
	{
		val intent = Intent(
			if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
				Intent.ACTION_OPEN_DOCUMENT
			else
				Intent.ACTION_GET_CONTENT)
		intent.type = "*/*"
		intent.addCategory(Intent.CATEGORY_OPENABLE)
		try
		{
			this.startActivityForResult(
				Intent.createChooser(
					intent,
					"Select your .json dictionary file"),
				SELECT_DICTIONARY_FILE)
		}
		catch(e: android.content.ActivityNotFoundException)
		{
			Toast.makeText(
				this.activity,
				"Please install a File Manager.",
				Toast.LENGTH_LONG
			).show()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
	{
		if(data != null)
			when(requestCode)
			{
				SELECT_DICTIONARY_FILE ->
					if(resultCode == PreferenceActivity.RESULT_OK)
					{
						this.add(data.data.toString())
						// A resume will occur immediately after this,
						// which will reload the preference.
						this.save()
					}
			}
		super.onActivityResult(requestCode, resultCode, data)
	}

	fun reset()
	{
		val system = IRELAND_SYSTEM

		val defaultDictionaries = system.defaultDictionaries
			.map({ DictionaryItem(it, true) })
			.toJson()

		this.load(defaultDictionaries)
	}

	fun add(uri: String)
	{
		this.adapter?.run({
			if(this.items.all({ it.name != uri }))
				this.items.add(DictionaryItem(uri, true))
			this.notifyDataSetChanged()
		})
	}

	fun save()
	{
		val value = this.adapter?.items?.toJson()
		if(value != null)
			PreferenceManager.getDefaultSharedPreferences(this.activity)
				.edit()
				.putString(this.key, value)
				.apply()
	}

	fun load(value: String)
	{
		val newItems = dictionaryListFromJson(this.key, value)
		this.adapter?.clear()
		this.adapter?.addAll(newItems)
		this.adapter?.notifyDataSetChanged()
	}
}
