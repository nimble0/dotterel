// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import androidx.preference.PreferenceFragmentCompat

import nimble.dotterel.util.flatten

fun bindSystemPreferencesToActiveSystem(preferenceFragment: PreferenceFragmentCompat)
{
	val sharedPreferences = preferenceFragment.preferenceManager.sharedPreferences

	val system = sharedPreferences.getString("system", null)

	val systemDataStore = system
		?.let({
			makeJsonSystemDataStore(
				preferenceFragment.requireContext(),
				sharedPreferences,
				it)
		})

	val systemPreferences = preferenceFragment
		.preferenceScreen
		.flatten()
		.filter({ it.extras.getString("type") == "system" })
	if(systemDataStore == null)
		systemPreferences.forEach({ it.isEnabled = false })
	else
		systemPreferences.forEach({
			it.isEnabled = true
			when(it.extras.getString("store_type"))
			{
				"json_file" ->
				{
					it.key = it.extras.getString("key")
					it.preferenceDataStore = systemDataStore
				}
			}
		})
}
