package nimble.dotterel

import android.os.Bundle
import android.view.MenuItem

import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import androidx.preference.PreferenceFragmentCompat

val PREFERENCE_RESOURCES = listOf(
	R.xml.pref_machines
)

class DotterelSettings :
	AppCompatActivity(),
	PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{
	private val rootTitle: String
		by lazy { this.getString(R.string.pref_root) }

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		this.setContentView(R.layout.settings)
		this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

		for(resource in PREFERENCE_RESOURCES)
			PreferenceManager.setDefaultValues(this, resource, true)

		if(savedInstanceState == null)
		{
			val fragment = SettingsFragment()
			this.supportFragmentManager
				.beginTransaction()
				.add(R.id.preference_screen, fragment)
				.commit()
			this.supportActionBar?.title = rootTitle
		}
	}

	override fun onPreferenceStartFragment(
		caller: PreferenceFragmentCompat?,
		preference: Preference
	): Boolean
	{
		val fragment = this.supportFragmentManager.fragmentFactory
			.instantiate(this.classLoader, preference.fragment, preference.extras)
		this.supportFragmentManager
			.beginTransaction()
			.replace(R.id.preference_screen, fragment)
			.addToBackStack(preference.title?.toString())
			.commit()

		this.supportActionBar?.title = preference.title

		return true
	}

	override fun onBackPressed()
	{
		super.onBackPressed()

		val count = this.supportFragmentManager.backStackEntryCount
		this.supportActionBar?.title = if(count == 0)
				this.rootTitle
			else
				this.supportFragmentManager
					.getBackStackEntryAt(count - 1)
					.name
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean
	{
		return when(item.itemId)
		{
			android.R.id.home ->
			{
				this.onBackPressed()
				true
			}
			else ->
				super.onOptionsItemSelected(item)
		}
	}
}

class SettingsFragment : PreferenceFragmentCompat()
{
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.addPreferencesFromResource(R.xml.pref_root)
	}
}
