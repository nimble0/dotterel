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

import com.eclipsesource.json.*

import java.io.File

import nimble.dotterel.util.ui.bindSummaryToValue
import nimble.dotterel.util.ui.flatten

private val PREFERENCE_RESOURCES = listOf(
	R.xml.pref_root,
	R.xml.pref_machines
)

private val DEFAULT_JSON_PREFERENCES = mapOf(
	Pair(
		"machineConfig/On Screen",
		"""{
			"style": "Touch",
			"minTouchRadius": 0,
			"maxTouchRadius": 20,
			"padding": 5
		}""")
)

fun setDefaultSettings(context: Context)
{
	val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
	val activeSystem = sharedPreferences.getString("system", null)
	if(activeSystem == null)
	{
		val systemsFolder = File(context.filesDir, "systems")
		systemsFolder.mkdirs()
		val newSystemFile = File(systemsFolder, "My System.json")
		newSystemFile.createNewFile()
		newSystemFile.writer()
			.use({ output ->
				JsonObject()
					.add("base", "asset:/systems/ireland.english.json")
					.writeTo(output, PrettyPrint.indentWithTabs())
			})

		sharedPreferences
			.edit()
			.putString("system", newSystemFile.absolutePath)
			.apply()
	}

	for(resource in PREFERENCE_RESOURCES)
		PreferenceManager.setDefaultValues(context, resource, true)

	for((k, v) in DEFAULT_JSON_PREFERENCES)
	{
		val currentValue = Json.parse(sharedPreferences.getString(k, "{}")).asObject()
		val mergedValue = Json.parse(v).asObject().merge(currentValue)
		if(currentValue != mergedValue)
			sharedPreferences
				.edit()
				.putString(k, mergedValue.toString(PrettyPrint.indentWithTabs()))
				.apply()
	}
}

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

		setDefaultSettings(this)

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
			.instantiate(this.classLoader, preference.fragment)
		fragment.arguments = preference.extras
		(fragment as? PreferenceFragment)?.preference = preference
		this.supportFragmentManager
			.beginTransaction()
			.replace(R.id.preference_screen, fragment)
			.addToBackStack(preference.title?.toString())
			.commit()

		this.supportActionBar?.title = preference.title

		return true
	}

	fun exitFragment()
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

	override fun onBackPressed()
	{
		val fragment = this.supportFragmentManager
			.findFragmentById(R.id.preference_screen)
		if((fragment as? FragmentExitListener)?.onExit({ this.exitFragment() }) != false)
			this.exitFragment()
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

		this.preferenceScreen
			.flatten()
			.filter({ it.extras.getBoolean("bindSummaryToValue") })
			.forEach({ it.bindSummaryToValue() })

		this.findPreference<ListPreference>("system")!!.also({ preference ->
			val currentPreferenceChangeListener = preference.onPreferenceChangeListener
			preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener()
			{ p, v ->
				currentPreferenceChangeListener?.onPreferenceChange(p, v)
				bindSystemPreferencesToActiveSystem(this)
				true
			}
		})
	}

	private fun updateSystemsList()
	{
		val systems = File(this.requireContext().filesDir, "systems")
			.listFiles()
			?.filter({ it.isFile && it.extension == "json" })
			?.sorted()
			?: listOf()
		this.findPreference<ListPreference>("system")!!.also({ preference ->
			preference.entries = systems.map({ it.nameWithoutExtension }).toTypedArray()
			preference.entryValues = systems.map({ it.absolutePath }).toTypedArray()

			// Update preference to reflect changes to underlying SharedPreferences value
			val value = this.preferenceManager.sharedPreferences
				.getString(preference.key, null)
			preference.onPreferenceChangeListener?.onPreferenceChange(
				preference,
				value)
		})

		bindSystemPreferencesToActiveSystem(this)
	}

	override fun onResume()
	{
		super.onResume()

		this.updateSystemsList()
	}
}
