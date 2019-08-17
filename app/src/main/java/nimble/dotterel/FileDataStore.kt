// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context

import androidx.preference.PreferenceDataStore

class FileDataStore(context: Context) : PreferenceDataStore()
{
	private val resolver = AndroidSystemResources(context)

	var onPreferenceChanged: ((key: String) -> Unit)? = null

	private fun get(file: String): String? =
		this.resolver
			.openInputStream(file)
			?.bufferedReader()
			?.use({ it.readText() })

	private fun set(file: String, value: String?)
	{
		this.resolver
			.openOutputStream(file)
			?.bufferedWriter()
			?.use({ it.write(value) })

		this.onPreferenceChanged?.invoke(file)
	}

	override fun getBoolean(key: String, defValue: Boolean): Boolean =
		this.get(key)?.toBoolean() ?: defValue
	override fun getInt(key: String, defValue: Int): Int =
		this.get(key)?.toIntOrNull() ?: defValue
	override fun getLong(key: String, defValue: Long): Long =
		this.get(key)?.toLongOrNull() ?: defValue
	override fun getFloat(key: String, defValue: Float): Float =
		this.get(key)?.toFloatOrNull() ?: defValue
	override fun getString(key: String, defValue: String?): String? =
		this.get(key) ?: defValue

	override fun putBoolean(key: String, value: Boolean) =
		this.set(key, value.toString())
	override fun putInt(key: String, value: Int) =
		this.set(key, value.toString())
	override fun putLong(key: String, value: Long) =
		this.set(key, value.toString())
	override fun putFloat(key: String, value: Float) =
		this.set(key, value.toString())
	override fun putString(key: String, value: String?) =
		this.set(key, value)
}
