package nimble.dotterel

import android.annotation.TargetApi
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceManager

val PREFERENCE_RESOURCES = listOf(
	R.xml.pref_machines
)

class DotterelSettings : AppCompatPreferenceActivity()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

		for(resource in PREFERENCE_RESOURCES)
			PreferenceManager.setDefaultValues(this, resource, true)
	}

	override fun onIsMultiPane(): Boolean =
		(this.resources.configuration.screenLayout and
			Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE)

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	override fun onBuildHeaders(target: List<PreferenceActivity.Header>) =
		loadHeadersFromResource(R.xml.pref_headers, target)

	override fun isValidFragment(fragmentName: String): Boolean =
		fragmentName == PreferenceFragment::class.java.name
			|| fragmentName == DictionariesPreferenceFragment::class.java.name
			|| fragmentName == MachinesPreferenceFragment::class.java.name
}
