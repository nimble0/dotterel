// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.PrettyPrint

import java.io.File

import nimble.dotterel.util.ui.DialogPreference
import nimble.dotterel.util.ui.displayPreferenceDialog
import nimble.dotterel.util.ui.flatten

private const val DIALOG_FRAGMENT_TAG = "nimble.dotterel.SystemPreferenceFragment.DIALOG"

fun makeJsonSystemDataStore(
	context: Context,
	sharedPreferences: SharedPreferences,
	path: String
) =
	JsonSystemDataStore(
		AndroidSystemResources(context),
		path
	).also({
		it.onPreferenceChanged = {
			reloadSystem(sharedPreferences, path)
		}
		it.onGetError = { k, e ->
			val m = "Could not get $k: ${e.message}"
			Log.i("Dotterel Settings", m)
			Toast.makeText(context, m, Toast.LENGTH_LONG).show()
		}
		it.onSetError = { k, e ->
			val m = "Could not set $k: ${e.message}"
			Log.i("Dotterel Settings", m)
			Toast.makeText(context, m, Toast.LENGTH_LONG).show()
		}
	})

@Suppress("UNUSED")
class SystemPreferenceFragment : PreferenceFragmentCompat()
{
	private var path: String = ""
	private var jsonDataStore: JsonSystemDataStore? = null
	private var readOnly = true

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.addPreferencesFromResource(R.xml.pref_system)
		this.setHasOptionsMenu(true)

		this.path = this.arguments?.getString("path", null) ?: ""
		this.readOnly = this.arguments?.getBoolean("readOnly", false) ?: true

		val fileDataStore = FileDataStore(this.requireContext())
		fileDataStore.onPreferenceChanged = {
			reloadSystem(this.preferenceManager.sharedPreferences, this.path)
		}
		this.jsonDataStore = makeJsonSystemDataStore(
			this.requireContext(),
			this.preferenceManager.sharedPreferences,
			this.path)

		for(preference in this.preferenceScreen.flatten())
		{
			preference.extras.putBoolean("readOnly", this.readOnly)
			when(preference.extras.getString("store_type"))
			{
				"text_file" ->
				{
					preference.key = this.path
					preference.preferenceDataStore = fileDataStore
				}
				"json_file" ->
				{
					preference.key = preference.extras.getString("key")
					preference.preferenceDataStore = this.jsonDataStore
				}
			}
		}
	}

	override fun onResume()
	{
		super.onResume()
		this.jsonDataStore?.reload()
	}

	private fun rename() =
		AlertDialog.Builder(this.preferenceScreen.context).also({ alert ->
			val input = EditText(this.preferenceScreen.context)
			input.hint = this.getString(R.string.pref_systems_name)
			alert
				.setTitle(R.string.pref_systems_rename)
				.setPositiveButton(android.R.string.ok, { _, _ ->
					val newName = input.text.toString()

					val context = this.requireContext()

					val systemsFolder = File(context.filesDir, "systems")
					systemsFolder.mkdirs()
					val newFile = File(systemsFolder, "$newName.json")
					File(this.path).renameTo(newFile)

					val preference = Preference(this.preferenceScreen.context)
					preference.fragment = "nimble.dotterel.SystemPreferenceFragment"
					preference.key = newFile.absolutePath
					preference.title = newName
					preference.extras.putString("path", newFile.absolutePath)

					val activeSystem = this.preferenceManager.sharedPreferences
						.getString("system", null)
					if(activeSystem == this.path)
						this.preferenceManager.sharedPreferences
							.edit()
							.putString("system", newFile.absolutePath)
							.apply()

					(this.activity as? DotterelSettings)?.also({
						it.onBackPressed()
						it.onPreferenceStartFragment(this, preference)
					})
				})
				.setNegativeButton(android.R.string.cancel, { _, _ -> })
			alert.setView(input)
		}).create().show()

	private fun copy() =
		AlertDialog.Builder(this.preferenceScreen.context).also({ alert ->
			val input = EditText(this.preferenceScreen.context)
			input.hint = this.getString(R.string.pref_systems_name)
			alert
				.setTitle(R.string.pref_systems_new)
				.setPositiveButton(android.R.string.ok, { _, _ ->
					val newSystemName = input.text.toString()

					val context = this.requireContext()

					val systemsFolder = File(context.filesDir, "systems")
					systemsFolder.mkdirs()
					val newSystemFile = File(systemsFolder, "$newSystemName.json")
					newSystemFile.createNewFile()
					newSystemFile.writer()
						.use({ output ->
							JsonObject()
								.add("base", this.path)
								.writeTo(output, PrettyPrint.indentWithTabs())
						})

					val preference = Preference(this.preferenceScreen.context)
					preference.fragment = "nimble.dotterel.SystemPreferenceFragment"
					preference.key = newSystemFile.absolutePath
					preference.title = newSystemName
					preference.extras.putString("path", newSystemFile.absolutePath)

					(this.activity as? DotterelSettings)?.also({
						it.onBackPressed()
						it.onPreferenceStartFragment(this, preference)
					})
				})
				.setNegativeButton(android.R.string.cancel, { _, _ -> })
			alert.setView(input)
		}).create().show()

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
	{
		inflater.inflate(R.menu.system, menu)

		if(this.readOnly)
			menu.findItem(R.id.rename).isEnabled = false
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean =
		when(item.itemId)
		{
			R.id.rename ->
			{
				this.rename()
				true
			}
			R.id.copy ->
			{
				this.copy()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}

	override fun onDisplayPreferenceDialog(preference: Preference)
	{
		if(preference is DialogPreference)
			this.displayPreferenceDialog(preference, DIALOG_FRAGMENT_TAG)
		else
			super.onDisplayPreferenceDialog(preference)
	}
}
