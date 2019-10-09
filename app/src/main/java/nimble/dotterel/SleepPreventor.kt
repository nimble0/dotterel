// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager

import java.io.Closeable

class SleepPreventor(private val context: Context) : Closeable
{
	private var lastActivity: Long = 0L
	private var screenTimeout: Long = 0L

	private val handler = Handler(this.context.mainLooper)

	private val windowManager = this.context
		.getSystemService(Context.WINDOW_SERVICE) as WindowManager
	private val view: View = LayoutInflater.from(context)
		.inflate(R.layout.empty, null)

	private val screenTimeoutObserver = object : ContentObserver(this.handler)
	{
		override fun onChange(selfChange: Boolean)
		{
			this@SleepPreventor.updateScreenTimeout()
			this@SleepPreventor.checkWakeLock()
		}

		override fun deliverSelfNotifications(): Boolean = true
	}

	init
	{
		val windowParams = WindowManager.LayoutParams(
			0,
			0,
			0,
			0,
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
				@Suppress("DEPRECATION")
				(WindowManager.LayoutParams.TYPE_PHONE)
			else
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
			PixelFormat.TRANSLUCENT
		)
		windowParams.gravity = Gravity.START or Gravity.TOP

		this.handler.post({
			this.windowManager.addView(
				this.view,
				windowParams)
		})

		this.context.contentResolver.registerContentObserver(
			Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
			false,
			this.screenTimeoutObserver)
		this.updateScreenTimeout()
	}

	fun poke()
	{
		this.lastActivity = System.currentTimeMillis()
		this.handler.post({ this.view.keepScreenOn = true })
		this.handler.postDelayed(
			{ this.checkWakeLock() },
			this.screenTimeout)
	}

	private fun updateScreenTimeout()
	{
		this.screenTimeout = Settings.System.getLong(
			this.context.contentResolver,
			Settings.System.SCREEN_OFF_TIMEOUT,
			0)
	}

	private fun checkWakeLock()
	{
		if(System.currentTimeMillis() - this.lastActivity > this.screenTimeout)
			this.view.keepScreenOn = false
	}

	override fun close()
	{
		this.windowManager.removeView(this.view)
		this.context.contentResolver.unregisterContentObserver(
			this.screenTimeoutObserver)
	}
}
