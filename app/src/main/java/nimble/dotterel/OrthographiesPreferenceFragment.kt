// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.*
import android.widget.*

class OrthographyAssetBrowser : AssetBrowser()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		this.setRoots(listOf(
			AssetBrowserRoot(this.assets, "/orthography/")
		))

		this.navigate("asset:/orthography/")
	}
}

private const val SELECT_ORTHOGRAPHY_FILE = 2
private const val SELECT_ASSET_ORTHOGRAPHY_FILE = 3

class OrthographiesPreferenceFragment : PathListPreferenceFragment()
{
	override fun isAccessible(path: String): Boolean =
		this.systemManager.openOrthography(path) != null

	private fun chooseAssetOrthographyFile()
	{
		val intent = Intent(this.requireContext(), OrthographyAssetBrowser::class.java)
		intent.type = "*/*"
		intent.addCategory(Intent.CATEGORY_OPENABLE)
		intent.putExtra("initialPath", "asset:/orthography/")

		try
		{
			this.startActivityForResult(
				intent,
				SELECT_ASSET_ORTHOGRAPHY_FILE)
		}
		catch(_: ActivityNotFoundException)
		{
			Toast.makeText(
				this.requireContext(),
				"Please install a File Manager.",
				Toast.LENGTH_LONG
			).show()
		}
	}

	private fun chooseOrthographyFile()
	{
		val intent = Intent(
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
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
					"Select your .json orthography file"),
				SELECT_ORTHOGRAPHY_FILE)
		}
		catch(_: ActivityNotFoundException)
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
				SELECT_ORTHOGRAPHY_FILE ->
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
				SELECT_ASSET_ORTHOGRAPHY_FILE ->
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
				this.chooseAssetOrthographyFile()
				true
			}
			R.id.add_path ->
			{
				this.chooseOrthographyFile()
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
