// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.*
import android.widget.*

import com.eclipsesource.json.*

import nimble.dotterel.translation.EMPTY_KEY_LAYOUT
import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.util.set

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

class DictionariesPreferenceFragment : PathListPreferenceFragment()
{
	private var keyLayout: KeyLayout = EMPTY_KEY_LAYOUT

	override fun isAccessible(path: String): Boolean =
		this.systemManager.openDictionary(this.keyLayout, path) != null

	override fun load()
	{
		val preference = this.preference!!
		val dataStore = (preference.preferenceDataStore as? JsonDataStore)
		this.keyLayout = dataStore
			?.safeGet(
				"keyLayout",
				EMPTY_KEY_LAYOUT,
				{ KeyLayout(it.asObject()) })
			?: EMPTY_KEY_LAYOUT

		super.load()
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
		catch(_: android.content.ActivityNotFoundException)
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
		catch(_: android.content.ActivityNotFoundException)
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

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
	{
		inflater.inflate(R.menu.path_list, menu)

		if(this.readOnly)
		{
			menu.findItem(R.id.add_asset_path).isEnabled = false
			menu.findItem(R.id.add_path).isEnabled = false
			menu.findItem(R.id.reset).isEnabled = false
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean =
		when(item.itemId)
		{
			R.id.add_asset_path ->
			{
				this.chooseAssetDictionaryFile()
				true
			}
			R.id.add_path ->
			{
				this.chooseDictionaryFile()
				true
			}
			R.id.reset ->
			{
				this.reset()
				this.view?.actionMode?.finish()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
}
