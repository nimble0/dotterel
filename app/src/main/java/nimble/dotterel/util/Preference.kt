// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager

private val bindPreferenceSummaryToValueListener =
	Preference.OnPreferenceChangeListener{ preference, value ->

		val stringValue = value.toString()

		when(preference)
		{
			is ListPreference ->
			{
				val index = preference.findIndexOfValue(stringValue)
				preference.setSummary(
					if(index >= 0)
						preference.entries[index]
					else
						null)
			}
			else -> preference.summary = stringValue
		}
		true
	}

fun Preference.bindSummaryToValue()
{
	this.onPreferenceChangeListener = bindPreferenceSummaryToValueListener
	bindPreferenceSummaryToValueListener.onPreferenceChange(this,
		PreferenceManager
			.getDefaultSharedPreferences(this.context)
			.getString(this.key, ""))
}
