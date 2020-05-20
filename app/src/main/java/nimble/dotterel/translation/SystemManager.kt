// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.*

import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

import kotlin.system.measureTimeMillis

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import nimble.dotterel.translation.dictionaries.*
import nimble.dotterel.translation.orthographies.*
import nimble.dotterel.util.CaseInsensitiveString

interface SystemResources
{
	val transforms: Map<
		CaseInsensitiveString,
		(FormattedText, UnformattedText, Boolean) -> UnformattedText>
	val commands: Map<
		CaseInsensitiveString,
		(Translator, String) -> TranslationPart>
	val codeDictionaries: Map<String, (KeyLayout) -> Dictionary>

	fun openInputStream(path: String): InputStream?
	fun openOutputStream(path: String): OutputStream?
	fun isReadOnly(path: String): Boolean
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

	private var cachedDictionaries: MutableMap<
		Pair<String, KeyLayout>,
		WeakReference<Dictionary>> =
		mutableMapOf()
	private val cachedDictionariesMutex = Mutex()
	private var cachedOrthographies: MutableMap<String, WeakReference<Orthography>> =
		mutableMapOf()
	private val cachedOrthographiesMutex = Mutex()

	private suspend fun cacheDictionary(keyLayout: KeyLayout, path: String, dictionary: Dictionary)
	{
		this.cachedDictionariesMutex.withLock()
		{
			this.cachedDictionaries = this.cachedDictionaries
				.filterValues({ it.get() != null })
				.toMutableMap()
			this.cachedDictionaries[Pair(path, keyLayout)] =
				WeakReference(dictionary)
		}
	}

	fun openDictionary(keyLayout: KeyLayout, path: String): Dictionary? =
		runBlocking() { this@SystemManager.parallelOpenDictionary(keyLayout, path) }
	fun openOrthography(path: String): Orthography? =
		runBlocking() { this@SystemManager.parallelOpenOrthography(path) }

	suspend fun parallelOpenDictionary(keyLayout: KeyLayout, path: String): Dictionary?
	{
		val cached = this.cachedDictionariesMutex.withLock()
		{
			this.cachedDictionaries[Pair(path, keyLayout)]?.get()
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
					val dictionary = this.resources
						.codeDictionaries[path.substringAfter(":/")]
						?.invoke(keyLayout)
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
							?.let({
								if(this.resources.isReadOnly(path))
									ImmutableJsonDictionary.load(it, keyLayout)
								else
									JsonDictionary.load(it, keyLayout)
							})
							?.also({ dictionary: Dictionary ->
								this.cacheDictionary(keyLayout, path, dictionary)
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

		try
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
			return orthography
		}
		catch(e: FileParseException)
		{
			this.log.error("Error loading $path: ${e.message}")
		}

		return null
	}

	fun saveDictionary(path: String, dictionary: SaveableDictionary)
	{
		try
		{
			val saveTime = measureTimeMillis()
			{
				val output = this@SystemManager.resources.openOutputStream(path)
				if(output != null)
					dictionary.save(output)
			}
			this.log.info("Saved dictionary $path in ${saveTime}ms")
		}
		catch(e: java.io.IOException)
		{
			this.log.error("IO error writing dictionary $path: $e")
		}
	}

	fun parallelSaveDictionary(path: String, dictionary: SaveableDictionary)
	{
		if(dictionary.parallelSave)
			GlobalScope.launch()
			{
				this@SystemManager.saveDictionary(path, dictionary)
			}
		else
			this.saveDictionary(path, dictionary)
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
		override val codeDictionaries: Map<String, (KeyLayout) -> Dictionary> = mapOf()

		override fun openInputStream(path: String): InputStream? = null
		override fun openOutputStream(path: String): OutputStream? = null
		override fun isReadOnly(path: String): Boolean = false
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
