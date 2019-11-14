// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import nimble.dotterel.util.ui.flatten

class PermissionsPreferenceFragment : PreferenceFragmentCompat()
{
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
	{
		this.addPreferencesFromResource(R.xml.pref_permissions)

		val permissionHelper = PermissionsHelper(this.requireContext())

		this.preferenceScreen
			.flatten()
			.forEach({ preference ->
				val permission = preference.extras.getString("permission")
				if(permission != null)
				{
					val granted = permissionHelper.checkPermission(permission)

					preference.isEnabled = !granted
					preference.setIcon(
						if(granted)
							R.drawable.ic_check_circle_24dp
						else
							R.drawable.ic_error_24dp)

					preference.setOnPreferenceClickListener({
						permissionHelper.requestPermission(permission)
						true
					})
				}
			})
	}
}
