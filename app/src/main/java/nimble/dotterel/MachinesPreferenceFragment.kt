// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.os.Bundle

import androidx.preference.*

import nimble.dotterel.machines.ON_SCREEN_MACHINE_STYLES
import nimble.dotterel.util.*
import nimble.dotterel.util.DialogPreference

private const val DIALOG_FRAGMENT_TAG = "nimble.dotterel.MachinePreferenceFragment.DIALOG"

class MachinesPreferenceFragment : PreferenceFragmentCompat()
{
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.addPreferencesFromResource(R.xml.pref_machines)
		this.preferenceScreen.removePreference(this.findPreference("hidden"))

		val machinesCategory = this.preferenceScreen.findPreference("machines")
			as PreferenceCategory
		for(m in MACHINES)
		{
			val enabled = SwitchPreference(this.preferenceScreen.context)
			enabled.key = "machine/${m.key}"
			enabled.title = m.key
			enabled.setDefaultValue(false)
			machinesCategory.addPreference(enabled)
		}

		val onScreenStyle = this.findPreference("machine/On Screen/style")
			as ListPreference
		onScreenStyle.entries = ON_SCREEN_MACHINE_STYLES.map({ it.key })
			.toTypedArray()
		onScreenStyle.entryValues = onScreenStyle.entries
		onScreenStyle.bindSummaryToValue()
	}

	override fun onDisplayPreferenceDialog(preference: Preference)
	{
		if(preference is DialogPreference)
			this.displayPreferenceDialog(preference, DIALOG_FRAGMENT_TAG)
		else
			super.onDisplayPreferenceDialog(preference)
	}
}
