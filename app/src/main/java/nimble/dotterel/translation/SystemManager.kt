// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.Json
import com.eclipsesource.json.PrettyPrint

import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

import nimble.dotterel.util.CaseInsensitiveString

interface SystemResources
{
	val transforms: Map<
		CaseInsensitiveString,
		(FormattedText, UnformattedText, Boolean) -> UnformattedText>
	val commands: Map<
		CaseInsensitiveString,
		(Translator, String) -> TranslationPart>
	val codeDictionaries: Map<String, Dictionary>

	fun openInputStream(path: String): InputStream?
	fun openOutputStream(path: String): OutputStream?
}

class SystemManager(
	val resources: SystemResources,
	var log: (message: String) -> Unit = {})
{
	val transforms: Map<
		CaseInsensitiveString,
		(FormattedText, UnformattedText, Boolean) -> UnformattedText>
		get() = this.resources.transforms
	val commands: Map<
		CaseInsensitiveString,
		(Translator, String) -> TranslationPart>
		get() = this.resources.commands

	private var cachedDictionaries: MutableMap<String, WeakReference<Dictionary>> =
		mutableMapOf()
	private var cachedOrthographies: MutableMap<String, WeakReference<Orthography>> =
		mutableMapOf()

	fun openDictionary(path: String): Dictionary?
	{
		val cached = this.cachedDictionaries[path]?.get()
		if(cached != null)
			return cached

		try
		{
			val type = path.substringBefore(":")
			return when(type)
			{
				"code_dictionary" -> this.resources.codeDictionaries[
					path.substringAfter(":/")]
				else -> this.resources.openInputStream(path)
					?.let({ input -> JsonDictionary(input) })
					?.also({ dictionary: Dictionary ->
						this.cachedDictionaries = this.cachedDictionaries
							.filterValues({ it.get() != null })
							.toMutableMap()
						this.cachedDictionaries[path] = WeakReference(dictionary)
					})
			}
		}
		catch(e: com.eclipsesource.json.ParseException)
		{
			this.log("Dictionary $path has badly formed JSON")
		}
		catch(e: java.lang.NullPointerException)
		{
			this.log("Invalid type found while reading dictionary $path")
		}
		catch(e: java.lang.UnsupportedOperationException)
		{
			this.log("Invalid type found while reading dictionary $path")
		}

		return null
	}
	fun openOrthography(path: String): Orthography?
	{
		val cached = this.cachedOrthographies[path]?.get()
		if(cached != null)
			return cached

		return try
		{
			this.resources.openInputStream(path)
				?.let({
					when
					{
						path.endsWith(".regex.json") ->
							RegexOrthography.fromJson(it)
						path.endsWith(".simple.json") ->
							SimpleOrthography.fromJson(it)
						else -> null
					}
				})
				?.also({ orthography: Orthography ->
					this.cachedOrthographies = this.cachedOrthographies
						.filterValues({ it.get() != null })
						.toMutableMap()
					this.cachedOrthographies[path] = WeakReference(orthography)
				})
		}
		catch(e: FileParseException)
		{
			this.log("Error loading $path: ${e.message}")
			null
		}
	}

	fun openSystem(path: String): System?
	{
		try
		{
			val file = this.resources.openInputStream(path) ?: return null
			val json = Json.parse(file.bufferedReader()).asObject()
			val baseSystem = json.getString("base", null)
				?.let({ this.openSystem(it) })
			val system = System.fromJson(this, baseSystem, json)
			return system.copy(path = path)
		}
		catch(e: com.eclipsesource.json.ParseException)
		{
			this.log("System $path has badly formed JSON")
		}
		catch(e: java.lang.NullPointerException)
		{
			this.log("Invalid type found while reading system $path")
		}
		catch(e: java.lang.UnsupportedOperationException)
		{
			this.log("Invalid type found while reading system $path")
		}
		catch(e: java.lang.IllegalArgumentException)
		{
			this.log("Invalid type found while reading system $path")
		}

		return null
	}
	fun saveSystem(system: System)
	{
		try
		{
			if(system.path != null)
				this.resources.openOutputStream(system.path)
					?.bufferedWriter()
					?.use({
						system
							.toJson()
							.writeTo(it, PrettyPrint.indentWithTabs())
					})
		}
		catch(e: com.eclipsesource.json.ParseException)
		{
			this.log("System ${system.path} has badly formed JSON")
		}
		catch(e: java.lang.UnsupportedOperationException)
		{
			this.log("Invalid type found while reading system ${system.path}")
		}
		catch(e: java.io.IOException)
		{
			this.log("IO error reading system ${system.path}: ${e.message}")
		}
	}
}

val NULL_SYSTEM_MANAGER = SystemManager(
	object : SystemResources
	{
		override val transforms: Map<
			CaseInsensitiveString,
			(FormattedText, UnformattedText, Boolean) -> UnformattedText> =
			mapOf()
		override val commands: Map<
			CaseInsensitiveString,
			(Translator, String) -> TranslationPart> =
			mapOf()
		override val codeDictionaries: Map<String, Dictionary> = mapOf()

		override fun openInputStream(path: String): InputStream? = null
		override fun openOutputStream(path: String): OutputStream? = null
	}
)
