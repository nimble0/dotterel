// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import nimble.dotterel.machines.SerialStenoMachine

import java.io.Closeable

class IntentForwarder(val context: Context) : Closeable
{
	private val broadcastReceiver = object : BroadcastReceiver()
	{
		val intentListeners: MutableMap<String, MutableSet<Dotterel.IntentListener>> = mutableMapOf()

		override fun onReceive(context: Context, intent: Intent)
		{
			val action = intent.action ?: return
			this.intentListeners[action]?.forEach({ it.onIntent(context, intent) })
		}
	}

	init
	{
		this.context.registerReceiver(this.broadcastReceiver, IntentFilter())
	}

	private fun registerBroadcastReceiver()
	{
		this.context.unregisterReceiver(this.broadcastReceiver)
		this.context.registerReceiver(
			this.broadcastReceiver,
			IntentFilter().also({
				for(a in this.broadcastReceiver.intentListeners)
					it.addAction(a.key)
			}))
	}

	fun add(action: String, listener: Dotterel.IntentListener)
	{
		this.broadcastReceiver.intentListeners
			.getOrPut(action, { mutableSetOf() })
			.add(listener)
		this.registerBroadcastReceiver()
	}

	fun remove(action: String, listener: Dotterel.IntentListener)
	{
		this.broadcastReceiver.intentListeners[action]?.remove(listener)
		this.registerBroadcastReceiver()
	}

	override fun close()
	{
		this.context.unregisterReceiver(this.broadcastReceiver)
	}
}
