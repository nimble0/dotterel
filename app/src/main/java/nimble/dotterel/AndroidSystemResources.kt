// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.net.Uri
import android.util.Log

import java.io.*

import nimble.dotterel.translation.*
import nimble.dotterel.util.CaseInsensitiveString

private val CODE_DICTIONARIES = mapOf(
	Pair("Numbers", NumbersDictionary())
)

private val ANDROID_COMMANDS = mapOf(
	Pair("IME:EDITOR_ACTION", ::editorAction),
	Pair("IME:SWITCH_PREVIOUS", ::switchPreviousIme),
	Pair("IME:SWITCH_NEXT", ::switchNextIme),
	Pair("IME:SWITCH", ::switchIme),
	Pair("IME:SHOW_PICKER", ::showImePicker)
).mapKeys({ CaseInsensitiveString(it.key) })

class AndroidSystemResources(private val context: Context) : SystemResources
{
	override val transforms = TRANSFORMS
	override val commands = COMMANDS + ANDROID_COMMANDS
	override val codeDictionaries = CODE_DICTIONARIES

	override fun openInputStream(path: String): InputStream?
	{
		try
		{
			val type = path.substringBefore(":")
			return when(type)
			{
				"asset" -> this.context.assets.open(path.substringAfter(":/"))
				"content" -> this.context
					.contentResolver
					.openInputStream(Uri.parse(path))
				else -> File(path).inputStream()
			}
		}
		catch(e: FileNotFoundException)
		{
			Log.e("Dotterel", "File $path not found")
		}
		catch(e: IOException)
		{
			Log.e("Dotterel", "IO Exception: ${e.message}")
		}
		catch(e: SecurityException)
		{
			Log.i("Dotterel", "Permission denied reading dictionary $path")
		}

		return null
	}

	override fun openOutputStream(path: String): OutputStream?
	{
		try
		{
			val type = path.substringBefore(":")
			return when(type)
			{
				"asset" -> throw IOException("$path is read only")
				"content" -> this.context
					.contentResolver
					.openOutputStream(Uri.parse(path))
				else -> File(path).outputStream()
			}
		}
		catch(e: FileNotFoundException)
		{
			Log.e("Dotterel", "File $path not found")
		}
		catch(e: IOException)
		{
			Log.e("Dotterel", "IO error reading $path: ${e.message}")
		}
		catch(e: SecurityException)
		{
			Log.e("Dotterel", "Permission error reading $path: ${e.message}")
		}

		return null
	}
}
