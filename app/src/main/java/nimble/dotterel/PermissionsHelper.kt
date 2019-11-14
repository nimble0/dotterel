// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

class PermissionsHelper(val context: Context)
{
	fun checkAllPermissions(): Boolean =
		PERMISSIONS.all({ this.checkPermission(it) })

	fun requestPermission(permission: String) =
		when(permission)
		{
			"overlays" -> this.requestOverlayPermission()
			else -> Unit
		}

	fun checkPermission(permission: String): Boolean =
		when(permission)
		{
			"overlays" -> this.checkOverlayPermission()
			else -> false
		}

	fun checkOverlayPermission(): Boolean =
		Build.VERSION.SDK_INT < Build.VERSION_CODES.M
			|| Settings.canDrawOverlays(this.context)

	fun requestOverlayPermission()
	{
		if(!this.checkOverlayPermission())
			this.context.startActivity(Intent(
				Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
				Uri.parse("package:${this.context.packageName}")
			))
	}

	companion object
	{
		val PERMISSIONS = listOf(
			"overlays"
		)
	}
}
