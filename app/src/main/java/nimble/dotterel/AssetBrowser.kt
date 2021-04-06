// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*

import androidx.appcompat.app.AppCompatActivity

import com.eclipsesource.json.JsonObject

import kotlin.math.max

import nimble.dotterel.util.get

data class PathItem(
	val path: String,
	val name: String,
	val isFolder: Boolean)
{
	override fun toString() = this.name
}

private fun AssetManager.isPathFolder(path: String): Boolean =
	this.list(path)?.isNotEmpty() == true

private fun assetPath(path: String): String =
	if(path == "/")
		""
	else
		path.substring(
			if(path.startsWith("/")) "/".length else 0,
			path.length - if(path.endsWith("/")) "/".length else 0)

interface BrowserRoot
{
	val name: String
	val protocol: String
	val root: String
	fun files(path: String): List<PathItem>?
}

fun String.withoutPrefix(prefix: String): String? =
	if(this.startsWith(prefix))
		this.substring(prefix.length)
	else
		null

class AssetBrowserRoot(
	private val assetManager: AssetManager,
	override val root: String
) :
	BrowserRoot
{
	override val name = "Assets"
	override val protocol = "asset"

	override fun files(path: String): List<PathItem>?
	{
		val path2 = path.withoutPrefix("${this.protocol}:/")
			?.let({ assetPath(it )})
			?: return listOf()
		return this.assetManager
			.list(path2)
			?.map({
				PathItem(
					path + it,
					it,
					this.assetManager.isPathFolder(assetPath("$path2/$it")))
			})
	}
}

class JsonTreeBrowserRoot(
	val json: JsonObject,
	override val name: String,
	override val protocol: String,
	override val root: String
) :
	BrowserRoot
{
	override fun files(path: String): List<PathItem>?
	{
		val path2 = path.withoutPrefix("${this.protocol}:/")
			?.let({ assetPath(it) })
			?: return listOf()
		val pathSegments = if(path2 == "") listOf() else path2.split("/")
		return (this.json.get(pathSegments) as? JsonObject)
			?.map({ PathItem(path + it.name, it.name, it.value is JsonObject) })
	}
}

private class AssetPathAdapter(
	context: Context,
	items: MutableList<PathItem> = mutableListOf()
) :
	ArrayAdapter<PathItem>(context, R.layout.asset_browser_item, items)
{
	override fun getView(position: Int, convertView: View?, parent: ViewGroup)
		: View
	{
		val view = convertView ?: LayoutInflater.from(this.context)
			.inflate(R.layout.asset_browser_item, parent, false)
		val item = this.getItem(position) ?: return view

		view.findViewById<TextView>(android.R.id.title).text = item.name
		view.findViewById<TextView>(android.R.id.summary).also({
			it.visibility = if(item.isFolder) View.GONE else View.VISIBLE
			it.text = item.path
		})

		return view
	}
}

private const val ROOT_MENU_START_ID = 100

abstract class AssetBrowser : AppCompatActivity()
{
	private var roots: Map<String, BrowserRoot> = mapOf()
	private var rootIds: Map<Int, String> = mapOf()

	fun setRoots(roots: List<BrowserRoot>)
	{
		this.roots = roots.map({ Pair(it.protocol, it) }).toMap()
		this.rootIds = roots
			.mapIndexed({ i, it -> Pair(ROOT_MENU_START_ID + i, it.protocol) })
			.toMap()
	}

	private var path = ""
	private val pathFiles = mutableListOf<PathItem>()
	private var adapter: ArrayAdapter<PathItem>? = null

	fun navigate(path: String)
	{
		val protocol = path.substringBefore(":")
		val root = this.roots[protocol] ?: return

		this.path = if(path.endsWith("/")) path else "$path/"
		this.pathFiles.clear()
		this.pathFiles.addAll(root.files(this.path) ?: listOf())
		this.adapter?.notifyDataSetChanged()

		this.supportActionBar?.title = this.path
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.asset_browser)
		this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val listView = this.findViewById<ListView>(android.R.id.list)
		this.adapter = AssetPathAdapter(this, pathFiles)
		listView.adapter = this.adapter
		listView.onItemClickListener = AdapterView.OnItemClickListener{
			_, _, position, _ ->

			val item = this.pathFiles[position]
			if(item.isFolder)
				this.navigate(item.path)
			else
			{
				this.setResult(
					RESULT_OK,
					Intent().also({
						it.data = Uri.parse(item.path)
					}))
				this.finish()
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean
	{
		this.menuInflater.inflate(R.menu.asset_browser, menu)
		var i = 0
		for((id, protocol) in this.rootIds)
			menu.add(R.id.select_root, id, i++, this.roots[protocol]!!.name)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean
	{
		if(item.itemId == android.R.id.home)
		{
			this.finish()
			return true
		}

		val root = this.roots[this.rootIds[item.itemId]]
		return if(root != null)
		{
			this.navigate("${root.protocol}:${root.root}")
			true
		}
		else
			super.onOptionsItemSelected(item)
	}

	override fun onBackPressed()
	{
		val protocol = path.substringBefore(":")
		val root = this.roots[protocol] ?: return
		val rootPath = "${root.protocol}:${root.root}"

		if(this.path == rootPath)
			super.onBackPressed()
		else
		{
			val parent = (this.path.substring(
					0,
					max(0, this.path.lastIndexOf(
						"/",
						this.path.length - 1 - "/".length)))
				+ "/")
			if(parent.startsWith(rootPath))
				this.navigate(parent)
		}
	}
}
