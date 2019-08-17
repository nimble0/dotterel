// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.SharedPreferences

import com.eclipsesource.json.*

import nimble.dotterel.util.get
import nimble.dotterel.util.setNotNull

class JsonPreferenceDataStore(
	private val sharedPreferences: SharedPreferences,
	private val preferenceKey: String
) :
	JsonDataStore()
{
	private var json: JsonObject
		get() = Json.parse(
			this.sharedPreferences.getString(this.preferenceKey, "{}"))
			.asObject()
		set(value) =
			this.sharedPreferences
				.edit()
				.putString(
					this.preferenceKey,
					value.toString(PrettyPrint.indentWithTabs()))
				.apply()

	override fun get(key: String): JsonValue? =
		this.json.get(splitJsonKey(key))

	override fun put(key: String, value: JsonValue?)
	{
		val splitKey = splitJsonKey(key)
		this.json = json.also({ it.setNotNull(splitKey, value) })
		this.onPreferenceChanged?.invoke(splitKey)
	}
}
