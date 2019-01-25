// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import androidx.preference.PreferenceDataStore

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue

import nimble.dotterel.util.toJson

private const val KEY_DELIMITER = "/"

fun splitJsonKey(key: String) =
	key.replace("\\\\", 0xFFFE.toString())
		.replace("\\$KEY_DELIMITER", 0xFFFF.toString())
		.split(KEY_DELIMITER)
		.map({
			it.replace(0xFFFE.toString(), "\\")
				.replace(0xFFFF.toString(), KEY_DELIMITER)
		})

abstract class JsonDataStore : PreferenceDataStore()
{
	var onGetError: ((key: String, e: Exception) -> Unit)? = null
	var onSetError: ((key: String, e: Exception) -> Unit)? = null
	var onPreferenceChanged: ((key: List<String>) -> Unit)? = null

	protected abstract fun get(key: String) : JsonValue?
	protected abstract fun put(
		key: String,
		value: JsonValue?)

	fun <T> safeGet(key: String, defValue: T, f: (JsonValue) -> T?) =
		try
		{
			this.get(key)?.let({ f(it) }) ?: defValue
		}
		catch(e: Exception)
		{
			this.onGetError?.invoke(key, e)
			defValue
		}

	fun safePut(key: String, value: JsonValue?)
	{
		try
		{
			this.put(key, value)
		}
		catch(e: Exception)
		{
			this.onSetError?.invoke(key, e)
		}
	}

	override fun getBoolean(key: String, defValue: Boolean): Boolean =
		this.safeGet(key, defValue, { it.asBoolean() })
	override fun getInt(key: String, defValue: Int): Int =
		this.safeGet(key, defValue, { it.asInt() })
	override fun getLong(key: String, defValue: Long): Long =
		this.safeGet(key, defValue, { it.asLong() })
	override fun getFloat(key: String, defValue: Float): Float =
		this.safeGet(key, defValue, { it.asFloat() })
	override fun getString(key: String, defValue: String?): String? =
		this.safeGet(key, defValue, { it.asString() })
	override fun getStringSet(
		key: String,
		defValues: MutableSet<String>?
	): MutableSet<String>? =
		this.safeGet(
			key,
			defValues,
			{ v ->
				v.asArray()
					.map({ it.asString() })
					.toMutableSet()
			})

	override fun putBoolean(key: String, value: Boolean): Unit =
		this.safePut(key, Json.value(value))
	override fun putInt(key: String, value: Int): Unit =
		this.safePut(key, Json.value(value))
	override fun putLong(key: String, value: Long): Unit =
		this.safePut(key, Json.value(value))
	override fun putFloat(key: String, value: Float): Unit =
		this.safePut(key, Json.value(value))
	override fun putString(key: String, value: String?): Unit =
		this.safePut(key, Json.value(value))
	override fun putStringSet(key: String, values: MutableSet<String>?): Unit =
		this.safePut(key, values?.toList()?.toJson() ?: Json.NULL)
}
