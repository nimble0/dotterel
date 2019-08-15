// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.util.*
import android.view.*
import android.widget.*
import android.widget.AbsListView.*

import androidx.preference.PreferenceFragmentCompat

import com.eclipsesource.json.*

import java.io.IOException

import nimble.dotterel.translation.Dictionary
import nimble.dotterel.translation.systems.IRELAND_SYSTEM

data class DictionaryItem(
	var name: String,
	var enabled: Boolean,
	var accessible: Boolean = true)

private fun checkAccessible(context: Context, path: String): Boolean
{
	try
	{
		val type = path.substringBefore("://")
		val name = path.substringAfter("://")
		return when(type)
		{
			"asset" ->
			{
				context.assets.open(name)
				true
			}
			"code" ->
				CODE_ASSETS[name] is Dictionary
			else ->
			{
				context.contentResolver.openInputStream(Uri.parse(path))
				true
			}
		}
	}
	catch(e: IOException)
	{
		Log.w("Dotterel", "Error reading dictionary $path")
		return false
	}
	catch(e: SecurityException)
	{
		Log.w("Dotterel", "Permission denied reading dictionary $path")
		return false
	}
	catch(e: java.lang.IllegalStateException)
	{
		Log.w("Dotterel", "$path is not a valid JSON dictionary")
		return false
	}
	catch(e: ClassCastException)
	{
		Log.w("Dotterel", "$path is not of type Dictionary")
		return false
	}
}

fun List<DictionaryItem>.toJson(): JsonArray
{
	val json = JsonArray()
	for(item in this)
		json.add(JsonObject().also({
			it.add("name", item.name)
			it.add("enabled", item.enabled)
		}))
	return json
}

fun dictionaryListFromJson(key: String, json: String): List<DictionaryItem>
{
	try
	{
		val dictionaryList = mutableListOf<DictionaryItem>()
		for(item in Json.parse(json).asArray())
			dictionaryList.add(item.asObject().let({
				DictionaryItem(
					it.get("name").asString(),
					it.get("enabled").asBoolean())
			}))
		return dictionaryList
	}
	catch(e: com.eclipsesource.json.ParseException)
	{
		Log.e("Dotterel", "Preference $key has badly formed JSON")
	}
	catch(e: java.lang.UnsupportedOperationException)
	{
		Log.e("Dotterel", "Invalid type found while reading preference $key")
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

		val item = this.getItem(position) ?: return v

		v.findViewById<TextView>(R.id.path)
			.also({
				it.text = item.name
				it.isEnabled = item.enabled
			})

		val background = if(item.accessible)
			R.drawable.dictionary_item
		else
			R.drawable.dictionary_item_inaccessible
		v.background =
			if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
				this.context.resources.getDrawable(background, null)
			else
				@Suppress("DEPRECATION")
				this.context.resources.getDrawable(background)

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
		mode.menuInflater.inflate(R.menu.dictionaries_preference, menu)
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

const val SELECT_DICTIONARY_FILE = 2

class DictionariesPreferenceFragment : PreferenceFragmentCompat()
{
	private var key = "system/Ireland/dictionaries"
	private var view: ReorderableListView? = null
	private var adapter: DictionariesPreferenceAdapter? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.key = "system/Ireland/dictionaries"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View
	{
		val view = inflater.inflate(R.layout.pref_dictionaries, container, false)

		val adapter = DictionariesPreferenceAdapter(inflater.context)

		val listView = view.findViewById<ReorderableListView>(R.id.dictionaries)
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
			this.view?.actionMode?.finish()
		})

		return view
	}

	override fun onResume()
	{
		super.onResume()

		val value = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
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

	override fun onDestroyView()
	{
		this.view?.actionMode?.finish()
		super.onDestroyView()
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
				this.requireContext(),
				"Please install a File Manager.",
				Toast.LENGTH_LONG
			).show()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
	{
		val uri = data?.data
		if(uri != null)
			when(requestCode)
			{
				SELECT_DICTIONARY_FILE ->
					if(resultCode == PreferenceActivity.RESULT_OK)
					{
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
							this.requireContext()
								.contentResolver
								.takePersistableUriPermission(
									uri,
									Intent.FLAG_GRANT_READ_URI_PERMISSION
										or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

						this.add(uri.toString())
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
			.toString()

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
			PreferenceManager.getDefaultSharedPreferences(this.requireContext())
				.edit()
				.putString(this.key, value.toString())
				.apply()
	}

	fun load(value: String)
	{
		val newItems = dictionaryListFromJson(this.key, value)
			.map({
				DictionaryItem(
					it.name,
					it.enabled,
					checkAccessible(this.requireContext(), it.name))
			})
		this.adapter?.clear()
		this.adapter?.addAll(newItems)
		this.adapter?.notifyDataSetChanged()
	}
}
