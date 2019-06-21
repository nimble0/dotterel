// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.*

import kotlinx.coroutines.*

import nimble.dotterel.util.*

data class SystemDictionary(
	val path: String,
	val enabled: Boolean,
	val dictionary: Dictionary)
{
	fun toJson() = JsonObject().also({
		it.set("path", this.path)
		it.set("enabled", this.enabled)
	})

	companion object
	{
		fun fromJson(manager: SystemManager, keyLayout: KeyLayout, json: JsonObject) =
			manager.openDictionary(keyLayout, json.get("path").asString())
				?.let({ dictionary ->
					SystemDictionary(
						json.get("path").asString(),
						json.get("enabled").asBoolean(),
						dictionary)
				})
	}
}

data class SystemOrthography(
	val path: String,
	val enabled: Boolean,
	val orthography: Orthography)
{
	fun toJson() = JsonObject().also({
		it.set("path", this.path)
		it.set("enabled", this.enabled)
	})

	companion object
	{
		fun fromJson(manager: SystemManager, json: JsonObject) =
			manager.openOrthography(json.get("path").asString())
				?.let({ orthography ->
					SystemOrthography(
						json.get("path").asString(),
						json.get("enabled").asBoolean(),
						orthography)
				})
	}
}

data class SystemDictionaries(
	override val keyLayout: KeyLayout,
	val dictionaries: List<SystemDictionary> = listOf()
) :
	Dictionary
{
	override val longestKey: Int
		get() = this.dictionaries
			.asSequence()
			.map({ it.dictionary.longestKey })
			.max() ?: 0

	override fun get(k: List<Stroke>): String? =
		this.dictionaries
			.asSequence()
			.mapNotNull({
				if(it.enabled && k.size <= it.dictionary.longestKey)
					it.dictionary[k]
				else
					null
			})
			.firstOrNull()

	fun toJson() = this.dictionaries.map({ it.toJson() }).toJson()

	companion object
	{
		fun fromJson(manager: SystemManager, keyLayout: KeyLayout, json: JsonArray) =
			json.map({ it.asObject() })
				.mapNotNull({ SystemDictionary.fromJson(manager, keyLayout, it) })
				.let({ SystemDictionaries(keyLayout, it) })
	}
}

data class SystemOrthographies(
	val orthographies: List<SystemOrthography> = listOf()
) :
	Orthography
{
	override fun match(a: String, b: String) =
		this.orthographies
			.filter({ it.enabled })
			.fold<SystemOrthography, Orthography.Result?>(
				null,
				{ acc, it -> acc ?: it.orthography.match(a, b) })

	fun toJson() = this.orthographies.map({ it.toJson() }).toJson()

	companion object
	{
		fun fromJson(manager: SystemManager, json: JsonArray) =
			json.map({ it.asObject() })
				.mapNotNull({ SystemOrthography.fromJson(manager, it) })
				.let({ SystemOrthographies(it) })
	}
}

fun defaultFormattingToJson(manager: SystemManager, formatting: Formatting) =
	JsonObject().also({
		it.set("space", formatting.space)
		it.set("spaceStart", formatting.spaceEnd.toString())
		it.set(
			"transform",
			manager.transforms.inverted()[formatting.transform]?.value)
		it.set(
			"singleTransform",
			manager.transforms.inverted()[formatting.singleTransform]?.value)
		it.set("transformState", formatting.transformState.toString())
	})

fun defaultFormattingFromJson(manager: SystemManager, json: JsonObject) =
	Formatting(
		space = json.getString("space", " "),
		spaceEnd = json.getOrNull("spaceStart")?.asString()
			?.let({ Formatting.Space.valueOf(it) })
			?: Formatting.Space.NORMAL,
		transform = manager.transforms[
			json.getOrNull("transform")
				?.asString()
				?.let({ CaseInsensitiveString(it) })],
		singleTransform = manager.transforms[
			json.getOrNull("singleTransform")
				?.asString()
				?.let({ CaseInsensitiveString(it) })],
		transformState = json.getOrNull("transformState")?.asString()
			?.let({ Formatting.TransformState.valueOf(it) })
			?: Formatting.TransformState.NORMAL)

data class System(
	val baseJson: JsonObject?,

	val path: String?,
	val manager: SystemManager,

	var keyLayout: KeyLayout = KeyLayout(""),
	var prefixStrokes: List<Stroke> = listOf(),
	var suffixStrokes: List<Stroke> = listOf(),

	var aliases: Map<CaseInsensitiveString, String> = mapOf(),

	var defaultFormatting: Formatting = Formatting(),

	var orthographies: SystemOrthographies = SystemOrthographies(listOf()),

	var dictionaries: SystemDictionaries = SystemDictionaries(EMPTY_KEY_LAYOUT),

	var machineConfig: JsonObject = JsonObject())
{
	val transforms: Map<
		CaseInsensitiveString,
		(FormattedText, UnformattedText, Boolean) -> UnformattedText>
		get() = this.manager.transforms
	val commands: Map<
		CaseInsensitiveString,
		(Translator, String) -> TranslationPart>
		get() = this.manager.commands

	fun save() = this.manager.saveSystem(this)
	fun saveDictionary(dictionary: SystemDictionary)
	{
		if(dictionary.dictionary is SaveableDictionary)
			this.manager.parallelSaveDictionary(dictionary.path, dictionary.dictionary)
	}

	companion object
}

fun System.toJson() = JsonObject().also({ system ->
	system.setNotNull("keyLayout", this.keyLayout.toJson())
	system.setNotNull("aliases", this.aliases.mapKeys({ it.key.value }).toJson())
	system.setNotNull("prefixStrokes", this.prefixStrokes.map({ it.rtfcre }).toJson())
	system.setNotNull("suffixStrokes", this.suffixStrokes.map({ it.rtfcre }).toJson())
	system.setNotNull(
		"defaultFormatting",
		defaultFormattingToJson(this.manager, this.defaultFormatting))
	system.setNotNull("orthographies", this.orthographies.toJson())
	system.setNotNull("dictionaries", this.dictionaries.toJson())
	system.setNotNull("machineConfig", this.machineConfig)

	if(this.baseJson != null)
	{
		for(member in this.baseJson)
			if(member.value == system[member.name])
				system.remove(member.name)

		val baseMachineConfig = this.baseJson["machineConfig"] as? JsonObject
		val machineConfig = system["machineConfig"] as? JsonObject
		if(baseMachineConfig != null && machineConfig != null)
			for(member in baseMachineConfig)
				if(member.value == system[member.name])
					machineConfig.remove(member.name)
	}
})

fun System.Companion.fromJson(
	manager: SystemManager,
	baseJson: JsonObject?,
	json: JsonObject
): System =
	runBlocking()
	{
		val mergedJson = baseJson?.also({ it.mergeSystem(json) }) ?: json
		val keyLayout = mergedJson
			.getOrNull("keyLayout")?.asObject()
			?.let({ KeyLayout(it) })
			?: NULL_SYSTEM.keyLayout

		data class LoadingOrthography(
			val path: String,
			val enabled: Boolean,
			val orthography: Deferred<Orthography?>
		)
		data class LoadingDictionary(
			val path: String,
			val enabled: Boolean,
			val dictionary: Deferred<Dictionary?>
		)

		val loadingOrthographies = mergedJson.getOrNull("orthographies")?.asArray()
			?.map({ it.asObject() })
			?.map({
				val path = it.get("path").asString()
				LoadingOrthography(
					path,
					it.get("enabled").asBoolean(),
					async(Dispatchers.Default) { manager.parallelOpenOrthography(path) })
			})

		val loadingDictionaries = mergedJson.getOrNull("dictionaries")?.asArray()
			?.map({ it.asObject() })
			?.map({
				val path = it.get("path").asString()
				LoadingDictionary(
					path,
					it.get("enabled").asBoolean(),
					async(Dispatchers.Default) { manager.parallelOpenDictionary(keyLayout, path) })
			})

		val orthographies = SystemOrthographies(
			loadingOrthographies?.mapNotNull({ orthography ->
				orthography.orthography.await()?.let({
					SystemOrthography(orthography.path, orthography.enabled, it)
				})
			}) ?: listOf())

		val dictionaries = SystemDictionaries(
			keyLayout,
			loadingDictionaries?.mapNotNull({ dictionary ->
				dictionary.dictionary.await()?.let({
					SystemDictionary(dictionary.path, dictionary.enabled, it)
				})
			}) ?: listOf())

		System(
			baseJson = baseJson,
			path = null,
			manager = manager,
			keyLayout = keyLayout,
			prefixStrokes = mergedJson.getOrNull("prefixStrokes")?.asArray()
				?.map({ keyLayout.parse(it.asString()) })
				?: listOf(),
			suffixStrokes = mergedJson.getOrNull("suffixStrokes")?.asArray()
				?.map({ keyLayout.parse(it.asString()) })
				?: listOf(),

			aliases = mergedJson.getOrNull("aliases")
				?.asObject()
				?.associateBy(
					{ CaseInsensitiveString(it.name) },
					{ it.value.asString() })
				?: mapOf(),

			defaultFormatting = mergedJson.getOrNull("defaultFormatting")?.asObject()
				?.let({ defaultFormattingFromJson(manager, it) })
				?: Formatting(),

			orthographies = orthographies,

			dictionaries = dictionaries,

			machineConfig = mergedJson.get("machineConfig")?.asObject() ?: JsonObject()
		)
	}

val NULL_SYSTEM = System(
	baseJson = null,
	path = null,
	manager = NULL_SYSTEM_MANAGER
)

fun JsonObject.mergeSystem(system: JsonObject)
{
	for(member in system)
		if(!member.value.isNull && member.name != "machineConfig")
			this.set(member.name, member.value)

	val machineConfig = this["machineConfig"] as? JsonObject ?: return
	val mergeMachineConfig = system["machineConfig"] as? JsonObject ?: return
	for(member in mergeMachineConfig)
		if(!member.value.isNull)
			machineConfig.set(member.name, member.value)
}
