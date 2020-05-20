// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util.ui

import androidx.preference.*

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

interface ReloadablePreference
{
	fun reloadValue()
}

fun Preference.reloadValue()
{
	when(this)
	{
		is ReloadablePreference ->
			this.reloadValue()
		is EditTextPreference ->
		{
			this.text = this.preferenceDataStore?.getString(this.key, null)
				?: this.text
			this.onPreferenceChangeListener?.onPreferenceChange(this, this.text)
		}
		is ListPreference ->
		{
			this.value = this.preferenceDataStore?.getString(this.key, null)
				?: this.value
			this.onPreferenceChangeListener?.onPreferenceChange(this, this.value)
		}
		is SeekBarPreference ->
		{
			this.value = this.preferenceDataStore?.getInt(this.key, this.value)
				?: this.value
			this.onPreferenceChangeListener?.onPreferenceChange(this, this.value)
		}
		is TwoStatePreference ->
		{
			this.isChecked = this.preferenceDataStore?.getBoolean(this.key, this.isChecked)
				?: this.isChecked
			this.onPreferenceChangeListener?.onPreferenceChange(this, this.isChecked)
		}
		is DropDownPreference ->
		{
			this.value = this.preferenceDataStore?.getString(this.key, null)
				?: this.value
			this.onPreferenceChangeListener?.onPreferenceChange(this, this.value)
		}
		is MultiSelectListPreference ->
		{
			this.values = this.preferenceDataStore?.getStringSet(this.key, null)
				?: this.values
			this.onPreferenceChangeListener?.onPreferenceChange(this, this.values)
		}
	}
}
