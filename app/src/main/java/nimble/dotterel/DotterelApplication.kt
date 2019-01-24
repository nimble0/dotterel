// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.app.*
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.FileProvider
import android.widget.Toast

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_FILE_NAME = "log"
private const val MAX_LOG_FILES = 10

@Suppress("UNUSED")
class DotterelApplication : Application()
{
	override fun onCreate()
	{
		super.onCreate()

		this.deleteOldLogFiles()

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			val crashChannel = NotificationChannel(
				"crash",
				this.getString(R.string.crash_notification_channel),
				NotificationManager.IMPORTANCE_DEFAULT)
			(this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
				.createNotificationChannel(crashChannel)
		}

		val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
		var crashing = false
		Thread.setDefaultUncaughtExceptionHandler({ thread, e ->
			if(!crashing)
			{
				crashing = true
				this.saveLogFile()?.also({ this.createLogNotification(it) })
			}

			defaultHandler.uncaughtException(thread, e)
		})
	}

	private fun deleteOldLogFiles()
	{
		this.getExternalFilesDir("logs")
			?.listFiles()
			?.filter({ it.name.startsWith(LOG_FILE_NAME) })
			?.sortedBy({ it.lastModified() })
			?.dropLast(MAX_LOG_FILES)
			?.forEach({ it.delete() })
	}

	private fun saveLogFile(): File?
	{
		val info: PackageInfo? = try
		{
			this.packageManager.getPackageInfo(this.packageName, 0)
		}
		catch(e: PackageManager.NameNotFoundException)
		{
			null
		}

		var model = Build.MODEL
		if(!model.startsWith(Build.MANUFACTURER))
			model = "${Build.MANUFACTURER} $model"

		val logFile = File(
			this.getExternalFilesDir("logs"),
			"$LOG_FILE_NAME${System.currentTimeMillis()}.txt")
		try
		{
			val process = Runtime.getRuntime().exec("logcat -d -v time")

			if(!logFile.createNewFile())
				throw IOException("Could not create file")
			logFile.writer().use({
				it.write("Time: ${Date()}\n"
					+ "Android version: ${Build.VERSION.SDK_INT}\n"
					+ "Device: $model\n"
					+ "App version: ${info?.versionName} (${info?.versionCode})\n")
				process.inputStream.reader().copyTo(it)
			})

			return logFile
		}
		catch(e: IOException)
		{
			Toast.makeText(
				this,
				this.getString(
					R.string.crash_notification_save_log_failed,
					e.localizedMessage),
				Toast.LENGTH_LONG
			).show()
		}

		return null
	}

	private fun createLogNotification(logFile: File)
	{
		val contentUri = FileProvider.getUriForFile(
			this,
			"nimble.dotterel",
			logFile)

		val shareIntent = Intent(Intent.ACTION_SEND)
		shareIntent.type = "text/plain"
		shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
		shareIntent.putExtra(Intent.EXTRA_SUBJECT,
			this.getString(R.string.crash_notification_subject))
		shareIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK
			or Intent.FLAG_GRANT_READ_URI_PERMISSION)

		// Grant permission to access log to all possible share activities
		// since we don't know which will be chosen.
		val intentActivities = this.packageManager.queryIntentActivities(
			shareIntent,
			PackageManager.MATCH_DEFAULT_ONLY)
		for(activity in intentActivities)
			this.grantUriPermission(
				activity.activityInfo.packageName,
				contentUri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION)

		val currentDateTime = SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss",
				Locale.getDefault())
			.format(Date())

		val notification = NotificationCompat.Builder(this, "crash")
			.setSmallIcon(R.drawable.ic_menu_manage)
			.setContentTitle(this.getString(
				R.string.crash_notification_title,
				currentDateTime))
			.setContentText(this.getString(
				R.string.crash_notification_saved_log_to,
				logFile.absolutePath))
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentIntent(PendingIntent.getActivity(
				this,
				// Should be unique for each instance of this type of notification
				Date().time.toInt(),
				Intent.createChooser(
					shareIntent,
					this.getString(R.string.crash_notification_share)),
				0))

		NotificationManagerCompat.from(this)
			.notify(0, notification.build())
	}
}
