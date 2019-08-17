// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.*
import android.widget.*
import android.widget.AbsListView.*

import com.eclipsesource.json.*

import nimble.dotterel.util.toJson
import nimble.dotterel.util.set

data class DictionaryItem(
	var path: String,
	var enabled: Boolean,
	var accessible: Boolean = true)

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
				it.text = item.path
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
		mode.menuInflater.inflate(R.menu.dictionaries_context, menu)
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

class DictionaryAssetBrowser : AssetBrowser()
{
	private val systemManager get() =
		(this.application as DotterelApplication).systemManager

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		this.setRoots(listOf(
			AssetBrowserRoot(this.assets, "/dictionaries/"),
			JsonTreeBrowserRoot(
				JsonObject().also({ tree ->
					for(path in this.systemManager.resources.codeDictionaries)
					{
						val segments = path.key.split("/")
						if(segments.size > 1)
							tree.set(segments, true)
						else
							tree.set(segments[0], true)
					}
				}),
				"Code",
				"code_dictionary",
				"/")
		))

		this.navigate("asset:/dictionaries/")
	}
}

private const val SELECT_DICTIONARY_FILE = 2
private const val SELECT_ASSET_DICTIONARY_FILE = 3

class DictionariesPreferenceFragment : PreferenceFragment()
{
	private var readOnly = true
	private var view: ReorderableListView? = null
	private var adapter: DictionariesPreferenceAdapter? = null
	private val systemManager get() =
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
		val view = inflater.inflate(R.layout.pref_dictionaries, container, false)

		val adapter = DictionariesPreferenceAdapter(inflater.context)

		val listView = view.findViewById<ReorderableListView>(R.id.dictionaries)
		listView.adapter = adapter
		listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
		listView.setMultiChoiceModeListener(
			DictionariesPreferenceModalListener(listView, adapter))

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

	private fun chooseAssetDictionaryFile()
	{
		val intent = Intent(this.requireContext(), DictionaryAssetBrowser::class.java)
		intent.type = "*/*"
		intent.addCategory(Intent.CATEGORY_OPENABLE)
		intent.putExtra("initialPath", "asset:/dictionaries/")

		try
		{
			this.startActivityForResult(
				intent,
				SELECT_ASSET_DICTIONARY_FILE)
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
				SELECT_ASSET_DICTIONARY_FILE ->
					if(resultCode == PreferenceActivity.RESULT_OK)
					{
						this.add(uri.toString())
						// A resume will occur immediately after this,
						// which will reload the preference.
						this.save()
					}
			}
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun reset()
	{
		if(this.readOnly)
			return

		val preference = this.preference!!
		(preference.preferenceDataStore as? JsonDataStore)
			?.safePut(preference.key, null)

		this.load()
	}

	private fun add(uri: String)
	{
		this.adapter?.also({
			if(it.items.all({ dictionary -> dictionary.path != uri }))
			{
				it.items.add(DictionaryItem(uri, true))
				it.notifyDataSetChanged()
			}
		})
	}

	private fun save()
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

	private fun load()
	{
		val preference = this.preference!!
		val dictionaries = (preference.preferenceDataStore as? JsonDataStore)
			?.safeGet(
				preference.key,
				null,
				{ v ->
					v.asArray()
						.map({ it.asObject() })
						.map({
							val path = it.get("path").asString()
							DictionaryItem(
								path,
								it.get("enabled").asBoolean(),
								this.systemManager.openDictionary(path) != null)
						})
				})
			?: listOf()

		this.adapter?.clear()
		this.adapter?.addAll(dictionaries)
		this.adapter?.notifyDataSetChanged()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
		inflater.inflate(R.menu.dictionaries, menu)

	override fun onOptionsItemSelected(item: MenuItem): Boolean =
		when(item.itemId)
		{
			R.id.add_asset_dictionary ->
			{
				this.chooseAssetDictionaryFile()
				true
			}
			R.id.add_dictionary ->
			{
				this.chooseDictionaryFile()
				true
			}
			R.id.reset_dictionaries ->
			{
				this.reset()
				this.view?.actionMode?.finish()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
}
