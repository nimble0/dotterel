package nimble.dotterel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager

import androidx.appcompat.app.AlertDialog
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


		// If Dotterel is not enabled as an input method, offer to open the
		// keyboard settings screen to enable it.
		val inputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE)
			as InputMethodManager
		val dotterelService = this.application.applicationInfo.packageName + "/.Dotterel"
		if(inputMethodManager.enabledInputMethodList.none({ it.id == dotterelService }))
			AlertDialog.Builder(this).also({ alert ->
				alert.setMessage(R.string.input_method_disabled_alert)
					.setPositiveButton(R.string.yes, { _, _ ->
						this.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
					})
					.setNegativeButton(R.string.no, { _, _ -> })
			}).create().show()
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
