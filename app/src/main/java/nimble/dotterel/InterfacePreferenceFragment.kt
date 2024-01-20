// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.annotation.SuppressLint
import android.os.Bundle

import androidx.preference.PreferenceFragmentCompat

import nimble.dotterel.util.ui.*

class InterfacePreferenceFragment : PreferenceFragmentCompat()
{
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.addPreferencesFromResource(R.xml.pref_interface)

		this.preferenceScreen
			.flatten()
			.filter({ it.extras.getBoolean("bindSummaryToValue") })
			.forEach({ it.bindSummaryToValue() })
	}

	@SuppressLint("ApplySharedPref")
	override fun onResume()
	{
		super.onResume()

		val jsonDataStores = mutableMapOf<String, JsonDataStore>()
		for(preference in this.preferenceScreen.flatten())
		{
			val dataStorePath = preference.extras.getString("store_path")
			if(dataStorePath != null)
				when(preference.extras.getString("store_type"))
				{
					"json_preference" ->
					{
						val dataStore = jsonDataStores[dataStorePath]
							?: JsonPreferenceDataStore(
								this.preferenceManager.sharedPreferences!!,
								dataStorePath
							).also({ jsonDataStores[dataStorePath] = it })
						preference.key = preference.extras.getString("key")
						preference.preferenceDataStore = dataStore

						preference.reloadValue()
					}
				}
		}
	}
}
