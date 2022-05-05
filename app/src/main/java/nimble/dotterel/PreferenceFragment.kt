// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

abstract class PreferenceFragment : PreferenceFragmentCompat()
{
	open var preference: Preference? = null

	fun loadPreference(): Preference?
	{
		val arguments = this.arguments ?: return null

		val storeType = arguments.get("store_type") as? String ?: return null
		when(storeType)
		{
			"json_file" ->
			{
				val key = arguments.get("key") as? String ?: return null
				val path = arguments.get("path") as? String ?: return null
				val systemDataStore = makeJsonSystemDataStore(
					this.requireContext(),
					this.preferenceManager.sharedPreferences,
					path)
				val preference = Preference(this.requireContext())
				preference.key = key
				preference.preferenceDataStore = systemDataStore
				return preference
			}
			"text_file" ->
			{
				val path = arguments.get("path") as? String ?: return null
				val fileDataStore = FileDataStore(this.requireContext())
				fileDataStore.onPreferenceChanged = {
					reloadSystem(this.preferenceManager.sharedPreferences, path)
				}
				val preference = Preference(this.requireContext())
				preference.key = path
				preference.preferenceDataStore = fileDataStore
				return preference
			}
			else ->
				return null
		}
	}
}
