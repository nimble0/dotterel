// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.os.Bundle

import androidx.preference.*

import nimble.dotterel.util.ui.*
import nimble.dotterel.util.ui.DialogPreference

private const val DIALOG_FRAGMENT_TAG = "nimble.dotterel.MachinePreferenceFragment.DIALOG"

class MachinesPreferenceFragment : PreferenceFragmentCompat(), StenoMachineTracker
{
	override val androidContext: Context
		get() = this.requireContext()
	override val intentForwarder by lazy { IntentForwarder(this.requireContext()) }

	private val machinesCategory by lazy {
		this.preferenceScreen.findPreference<PreferenceCategory>("machines")!!
	}

	private var machineFactories = mapOf<String, StenoMachine.Factory>()

	override fun addMachine(nameId: Pair<String, String>)
	{
		val enabled = SwitchPreference(this.preferenceScreen.context)
		enabled.title = combineNameId(nameId)
		enabled.key = "machine/${combineNameId(nameId)}"
		enabled.setDefaultValue(false)
		this.machinesCategory.addPreference(enabled)

		when(nameId.first)
		{
			"Serial" ->
			{
				this.addPreferencesFromResource(R.xml.pref_serial_machine)
				this.findPreference<Preference>("serial_machine")!!.also({
					it.title = combineNameId(nameId)
					it.key = combineNameId(nameId) + "/config"

					for(p in it.flatten())
						p.extras.putString("key", "${nameId.second}/" + p.extras.getString("key"))
				})
			}
		}

		this.bindStuffs()
	}

	override fun removeMachine(nameId: Pair<String, String>)
	{
		this.machinesCategory.removePreference(
			this.machinesCategory.findPreference(
				"machine/${combineNameId(nameId)}"))
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.addPreferencesFromResource(R.xml.pref_machines)
		this.preferenceScreen.removePreference(this.findPreference("hidden"))

		this.machineFactories = MACHINE_FACTORIES.mapValues({ it.value() })
		for(f in this.machineFactories)
			f.value.tracker = this
	}

	override fun onDisplayPreferenceDialog(preference: Preference)
	{
		if(preference is DialogPreference)
			this.displayPreferenceDialog(preference, DIALOG_FRAGMENT_TAG)
		else
			super.onDisplayPreferenceDialog(preference)
	}

	private fun bindStuffs()
	{
		this.preferenceScreen
			.flatten()
			.filter({ it.extras.getBoolean("bindSummaryToValue") })
			.forEach({ it.bindSummaryToValue() })

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
								this.preferenceManager.sharedPreferences,
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

	override fun onResume()
	{
		super.onResume()

		this.bindStuffs()
	}

	override fun onDestroy()
	{
		this.intentForwarder.close()
		super.onDestroy()
	}
}
