// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.app.Activity

// This is dummy activity to receive USB_DEVICE_ATTACHED events,
// which is the only way to automatically gain permission to access
// a USB device when plugged in.
class UsbEventReceiverActivity : Activity()
{
	override fun onResume()
	{
		super.onResume()
		this.finish()
	}
}
