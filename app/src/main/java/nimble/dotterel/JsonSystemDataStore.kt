// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import com.eclipsesource.json.*

import nimble.dotterel.translation.mergedSystemJson
import nimble.dotterel.translation.SystemResources
import nimble.dotterel.util.getOrNull
import nimble.dotterel.util.setNotNull

class JsonSystemDataStore(
	private val resources: SystemResources,
	private val path: String
) :
	JsonDataStore()
{
	private var json: JsonObject = JsonObject()

	private var mergedJson: JsonObject = JsonObject()

	init { this.reload() }

	fun reload()
	{
		this.json = this.resources
			.openInputStream(this.path)
			?.bufferedReader()
			?.let({ Json.parse(it).asObject() })
			?: JsonObject()
		this.mergedJson =
			mergedSystemJson(
				this.resources,
				this.path)
				?: JsonObject()
	}

	override fun get(key: String) : JsonValue? =
		this.mergedJson.getOrNull(splitJsonKey(key))
	override fun put(
		key: String,
		value: JsonValue?)
	{
		val splitKey = splitJsonKey(key)
		val v = if(value?.isNull == true) null else value

		if(v == this.mergedJson.getOrNull(splitKey))
			return

		this.json.setNotNull(splitKey, v)
		this.resources
			.openOutputStream(this.path)
			?.writer()
			?.use({ this.json.writeTo(it, PrettyPrint.indentWithTabs()) })

		if(v == null)
			this.mergedJson = mergedSystemJson(
				this.resources,
				this.path)
				?: JsonObject()
		else
			this.mergedJson.setNotNull(splitKey, v)

		this.onPreferenceChanged?.invoke(splitKey)
	}
}
