// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.os.Bundle

import androidx.preference.*

import nimble.dotterel.util.ui.*
import nimble.dotterel.util.ui.DialogPreference

private const val DIALOG_FRAGMENT_TAG = "nimble.dotterel.MachinePreferenceFragment.DIALOG"

class MachinesPreferenceFragment : PreferenceFragmentCompat()
{
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.addPreferencesFromResource(R.xml.pref_machines)
		this.findPreference<Preference>("hidden")
			?.let({ this.preferenceScreen.removePreference(it) })

		val machinesCategory = this.preferenceScreen
			.findPreference<PreferenceCategory>("machines")!!
		for(m in MACHINES)
		{
			val enabled = SwitchPreference(this.preferenceScreen.context)
			enabled.key = "machine/${m.key}"
			enabled.title = m.key
			enabled.setDefaultValue(false)
			machinesCategory.addPreference(enabled)
		}

		this.preferenceScreen
			.flatten()
			.filter({ it.extras.getBoolean("bindSummaryToValue") })
			.forEach({ it.bindSummaryToValue() })
	}

	override fun onDisplayPreferenceDialog(preference: Preference)
	{
		if(preference is DialogPreference)
			this.displayPreferenceDialog(preference, DIALOG_FRAGMENT_TAG)
		else
			super.onDisplayPreferenceDialog(preference)
	}

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

		bindSystemPreferencesToActiveSystem(this)
	}
}
