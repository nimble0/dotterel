// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.*

import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

import kotlin.system.measureTimeMillis

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
	var log: Log = object : Log {})
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
	private val cachedDictionariesMutex = Mutex()
	private var cachedOrthographies: MutableMap<String, WeakReference<Orthography>> =
		mutableMapOf()
	private val cachedOrthographiesMutex = Mutex()

	fun openDictionary(path: String): Dictionary? =
		runBlocking() { this@SystemManager.parallelOpenDictionary(path) }
	fun openOrthography(path: String): Orthography? =
		runBlocking() { this@SystemManager.parallelOpenOrthography(path) }

	suspend fun parallelOpenDictionary(path: String): Dictionary?
	{
		val cached = this.cachedDictionariesMutex.withLock()
		{
			this.cachedDictionaries[path]?.get()
		}
		if(cached != null)
		{
			this.log.info("Loaded cached dictionary $path")
			return cached
		}

		try
		{
			val type = path.substringBefore(":")
			return when(type)
			{
				"code_dictionary" ->
				{
					val dictionary = this.resources.codeDictionaries[path.substringAfter(":/")]
					if(dictionary == null)
						this.log.error("Code dictionary $path not found")
					else
						this.log.info("Loaded code dictionary $path")
					dictionary
				}
				else ->
				{
					var dictionary: Dictionary? = null
					val loadTime = measureTimeMillis()
					{
						dictionary = this.resources.openInputStream(path)
							?.let({ JsonDictionary(it) })
							?.also({ dictionary: Dictionary ->
								this.cachedDictionariesMutex.withLock()
								{
									this.cachedDictionaries = this.cachedDictionaries
										.filterValues({ it.get() != null })
										.toMutableMap()
									this.cachedDictionaries[path] =
										WeakReference(dictionary)
								}
							})
					}
					if(dictionary == null)
						this.log.error("Dictionary $path not found")
					else
						this.log.info("Loaded dictionary $path in ${loadTime}ms")
					dictionary
				}
			}
		}
		catch(e: FileParseException)
		{
			this.log.error("Error parsing dictionary $path")
		}

		return null
	}
	suspend fun parallelOpenOrthography(path: String): Orthography?
	{
		val cached = this.cachedOrthographiesMutex.withLock()
		{
			this.cachedOrthographies[path]?.get()
		}
		if(cached != null)
		{
			this.log.info("Loaded cached orthography $path")
			return cached
		}

		return try
		{
			var orthography: Orthography? = null
			val loadTime = measureTimeMillis()
			{
				orthography = this.resources.openInputStream(path)
					?.let({
						when
						{
							path.endsWith(".regex.json") ->
								RegexOrthography.fromJson(it)
							path.endsWith(".simple.json") ->
								SimpleOrthography.fromJson(it)
							path.endsWith(".regex_wl.json") ->
								RegexWithWordListOrthography.fromJson(it)
							else -> null
						}
					})
					?.also({ orthography: Orthography ->
						this.cachedOrthographiesMutex.withLock()
						{
							this.cachedOrthographies = this.cachedOrthographies
								.filterValues({ it.get() != null })
								.toMutableMap()
							this.cachedOrthographies[path] = WeakReference(orthography)
						}
					})
			}
			if(orthography == null)
				this.log.error("Orthography $path not found")
			else
				this.log.info("Loaded orthography $path in ${loadTime}ms")
			orthography
		}
		catch(e: FileParseException)
		{
			this.log.error("Error loading $path: ${e.message}")
			null
		}
	}

	fun openSystem(path: String): System?
	{
		try
		{
			var system: System? = null
			val loadTime = measureTimeMillis()
			{
				system = this.resources.openInputStream(path)
					?.let({ input ->
						val json = Json.parse(input.bufferedReader()).asObject()
						val baseJson = json.getString("base", null)
							?.let({ mergedSystemJson(this.resources, it) })
						System.fromJson(this, baseJson, json).copy(path = path)
					})
			}
			if(system == null)
				this.log.error("System $path not found")
			else
				this.log.info("Loaded system $path in ${loadTime}ms")
			return system
		}
		catch(e: ParseException)
		{
			this.log.error("System $path has badly formed JSON")
		}
		catch(e: java.lang.NullPointerException)
		{
			this.log.error("Invalid type found while reading system $path")
		}
		catch(e: java.lang.UnsupportedOperationException)
		{
			this.log.error("Invalid type found while reading system $path")
		}
		catch(e: java.lang.IllegalArgumentException)
		{
			this.log.error("Invalid type found while reading system $path")
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
		catch(e: java.io.IOException)
		{
			this.log.error("IO error writing system ${system.path}: ${e.message}")
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

fun mergedSystemJson(resources: SystemResources, path: String): JsonObject?
{
	try
	{
		val loadSystem = { systemPath: String ->
			resources.openInputStream(systemPath)
				?.let({ Json.parse(it.bufferedReader()).asObject() })
		}

		val system = loadSystem(path) ?: return null
		val systems = mutableListOf(system)
		while(true)
		{
			val baseSystemPath = systems.last().getString("base", null) ?: break
			val baseSystem = loadSystem(baseSystemPath) ?: break
			systems.add(baseSystem)
		}

		return systems.reduceRight({ it, acc ->
			acc.mergeSystem(it)
			acc
		})
	}
	catch(e: ParseException)
	{
		return null
	}
}
