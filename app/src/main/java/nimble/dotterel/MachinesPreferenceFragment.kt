// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.PreferenceFragment

import nimble.dotterel.machines.ON_SCREEN_MACHINE_STYLES
import nimble.dotterel.util.*

class MachinesPreferenceFragment : PreferenceFragment()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		this.preferenceScreen = this.preferenceManager
			.createPreferenceScreen(this.activity)

		for(m in MACHINES)
		{
			val enabled = CheckBoxPreference(this.preferenceScreen.context)
			enabled.key = "machine/${m.key}"
			enabled.title = m.key
			enabled.setDefaultValue(false)
			this.preferenceScreen.addPreference(enabled)
		}

		this.addPreferencesFromResource(R.xml.pref_machines)
		this.preferenceScreen.removePreference(this.findPreference("hidden"))

		val onScreenStyle = this.findPreference("machine/On Screen/style")
			as ListPreference
		onScreenStyle.entries = ON_SCREEN_MACHINE_STYLES.map({ it.key })
			.toTypedArray()
		onScreenStyle.entryValues = onScreenStyle.entries
		onScreenStyle.bindSummaryToValue()
	}
}
