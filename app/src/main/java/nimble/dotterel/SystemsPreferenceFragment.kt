// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AbsListView.*

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import java.io.File

private data class SystemItem(
	val name: String,
	val path: String,
	var active: Boolean = false)
{
	override fun toString() = this.name
}

private fun isAssetPath(path: String) = path.startsWith("asset:/")

private class SystemsPreferenceModalListener(
	val list: ListView,
	val adapter: ArrayAdapter<SystemItem>,
	val systemsPreferenceFragment: SystemsPreferenceFragment
) :
	MultiChoiceModeListener
{
	var actionMode: ActionMode? = null

	override fun onItemCheckedStateChanged(
		mode: ActionMode,
		i: Int,
		id: Long,
		checked: Boolean)
	{
		val item = this.adapter.getItem(i)!!
		if(checked && isAssetPath(item.path))
			this.list.setItemChecked(i, false)

		mode.title = "${this.list.checkedItemCount} Selected"
	}

	override fun onActionItemClicked(mode: ActionMode, menu: MenuItem): Boolean
	{
		val selected = this.list.checkedItemPositions
		return when(menu.itemId)
		{
			R.id.list_delete ->
			{
				val activeSystem = this.systemsPreferenceFragment
					.preferenceManager.sharedPreferences
					.getString("system", null)
				(0 until this.adapter.count)
					.map({ this.adapter.getItem(it)!! })
					.filterIndexed({ i, _ -> selected[i] })
					.forEach({
						File(it.path).delete()
						if(activeSystem == it.path)
							this.systemsPreferenceFragment
								.preferenceManager
								.sharedPreferences
								.edit()
								.putString("system", "")
								.apply()
					})

				this.systemsPreferenceFragment.refreshSystems()

				mode.finish()
				true
			}
			else -> false
		}
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean
	{
		mode.menuInflater.inflate(R.menu.systems, menu)
		this.actionMode = mode
		return true
	}

	override fun onDestroyActionMode(mode: ActionMode)
	{
		this.actionMode = null
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
}

private class SystemsPreferenceAdapter(
	context: Context,
	items: MutableList<SystemItem> = mutableListOf()
) :
	ArrayAdapter<SystemItem>(
		context,
		R.layout.pref_system_item,
		android.R.id.text1,
		items)
{
	override fun getView(position: Int, convertView: View?, parent: ViewGroup)
		: View
	{
		val view = super.getView(position, convertView, parent)
		val textView = view.findViewById<TextView>(android.R.id.text1)

		val item = this.getItem(position)!!

		// Despite being deprecated, this is the only available getDrawable
		// function signature for API level 16.
		@Suppress("DEPRECATION")
		textView.background = when
		{
			item.active ->
				context.resources.getDrawable(R.drawable.system_item_active)
			isAssetPath(item.path) ->
				context.resources.getDrawable(R.drawable.system_item_template)
			else -> null
		}

		return view
	}
}

class SystemsPreferenceFragment : PreferenceFragmentCompat()
{
	private var systemsAdapter: SystemsPreferenceAdapter? = null
	private var systemsListener: SystemsPreferenceModalListener? = null

	private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener({
		_, key ->
		if(key == "system")
			this.refreshSystems()
	})

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		// Preference listener stored as member variable because
		// SharedPreferences holds listeners with weak pointers.
		this.preferenceManager.sharedPreferences
			.registerOnSharedPreferenceChangeListener(this.preferenceListener)
	}

	override fun onDestroy()
	{
		super.onDestroy()
		this.preferenceManager.sharedPreferences
			.unregisterOnSharedPreferenceChangeListener(this.preferenceListener)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View
	{
		val preferenceView = super.onCreateView(inflater, container, savedInstanceState)
			as LinearLayout
		this.preferenceScreen = this.preferenceManager
			.createPreferenceScreen(this.activity)

		val view = inflater.inflate(R.layout.pref_systems, container, false)
		preferenceView.addView(view)

		val systemsAdapter = SystemsPreferenceAdapter(this.requireContext())
		this.systemsAdapter = systemsAdapter

		val systems = view.findViewById<ListView>(R.id.systems)
		systems.adapter = systemsAdapter
		systems.setOnItemClickListener({ _, _, position, _ ->
			val system: SystemItem = systemsAdapter.getItem(position)!!

			val preference = Preference(this.preferenceScreen.context)
			preference.fragment = "nimble.dotterel.SystemPreferenceFragment"
			preference.title = system.name
			preference.extras.putString("path", system.path)
			preference.extras.putBoolean("readOnly", isAssetPath(system.path))

			(this.activity as? DotterelSettings)
				?.onPreferenceStartFragment(this, preference)
		})
		systems.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
		this.systemsListener = SystemsPreferenceModalListener(
			systems,
			systemsAdapter,
			this)
		systems.setMultiChoiceModeListener(this.systemsListener)

		this.refreshSystems()

		return preferenceView
	}

	override fun onDestroyView()
	{
		this.systemsListener?.actionMode?.finish()
		super.onDestroyView()
	}

	internal fun refreshSystems()
	{
		this.systemsAdapter?.also({ systemsAdapter ->
			val systems = File(this.requireContext().filesDir, "systems")
				.listFiles()
				?.filter({ it.isFile && it.extension == "json" })
				?.sorted()
				?: listOf()
			val templateSystems = this.requireContext()
				.assets
				.list("systems")
				?.filter({ it.endsWith(".json") })
				?.map({ SystemItem(
					getString(
						R.string.pref_systems_template_system_item,
						it.substring(0, it.length - ".json".length)),
					"asset:/systems/$it") })
				?.sortedWith(compareBy({ it.name }))
				?: listOf()
			val activeSystem = this.preferenceManager.sharedPreferences
				.getString("system", null)

			systemsAdapter.clear()
			systemsAdapter.addAll(systems.map({
				val active = activeSystem == it.absolutePath
				SystemItem(
					if(active)
						getString(R.string.pref_systems_active_system_item, it.nameWithoutExtension)
					else
						it.nameWithoutExtension,
					it.absolutePath,
					active)
			}))
			systemsAdapter.addAll(templateSystems)
			systemsAdapter.notifyDataSetChanged()
		})
	}
}
