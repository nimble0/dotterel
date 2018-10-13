// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager

private val bindPreferenceSummaryToValueListener =
	Preference.OnPreferenceChangeListener{ preference, value ->
		val s = value?.toString()
		preference.summary = when(preference)
		{
			is ListPreference ->
				preference.entries?.getOrNull(preference.findIndexOfValue(s))
			else ->
				s
		}
		true
	}

fun Preference.bindSummaryToValue()
{
	this.onPreferenceChangeListener = bindPreferenceSummaryToValueListener

	val value = when(this)
	{
		is ListPreference ->
			this.value
		else ->
			PreferenceManager
				.getDefaultSharedPreferences(this.context)
				.getString(this.key, "")
	}
	bindPreferenceSummaryToValueListener.onPreferenceChange(this, value)
}

val PreferenceGroup.preferences: List<Preference>
	get() = (0 until this.preferenceCount)
		.map({ this.getPreference(it) })

fun Preference.flatten(): List<Preference> =
	if(this is PreferenceGroup)
		this.preferences.flatMap({ it.flatten() })
	else
		listOf(this)
