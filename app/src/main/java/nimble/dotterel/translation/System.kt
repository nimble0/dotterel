// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import com.eclipsesource.json.ParseException

import nimble.dotterel.util.*

data class SystemDictionary(
	val path: String,
	val enabled: Boolean,
	val dictionary: Dictionary)

data class SystemOrthography(
	val path: String,
	val orthography: Orthography)

data class SystemDictionaries(
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
			.mapNotNull({ if(it.enabled) it.dictionary[k] else null })
			.firstOrNull()
}

class MachineConfig(
	val system: System,
	val json: JsonObject)
{
	operator fun get(key: String): JsonValue? =
		(this.json[key] ?: this.system.base?.machineConfig?.get(key))
			?.let({ if(it == Json.NULL) null else it })
	operator fun set(key: String, value: JsonValue)
	{
		this.json.set(key, value)
		this.system.save()
	}
}

class System(
	val base: System?,

	val path: String?,
	val manager: SystemManager,

	keyLayout: KeyLayout? = null,
	prefixStrokes: List<Stroke>? = null,
	suffixStrokes: List<Stroke>? = null,

	aliases: Map<CaseInsensitiveString, String>? = null,

	defaultFormatting: Formatting? = null,

	orthography: SystemOrthography? = null,

	dictionaries: SystemDictionaries? = null,

	machineConfig: JsonObject = JsonObject())
{
	var _keyLayout: KeyLayout? = keyLayout
		set(v)
		{
			field = v
			this.save()
		}
	var _prefixStrokes: List<Stroke>? = prefixStrokes
		set(v)
		{
			field = v
			this.save()
		}
	var _suffixStrokes: List<Stroke>? = suffixStrokes
		set(v)
		{
			field = v
			this.save()
		}

	var _aliases: Map<CaseInsensitiveString, String>? = aliases
		set(v)
		{
			field = v
			this.save()
		}

	var _defaultFormatting: Formatting? = defaultFormatting
		set(v)
		{
			field = v
			this.save()
		}

	var _orthography: SystemOrthography? = orthography
		set(v)
		{
			field = v
			this.save()
		}

	var _dictionaries: SystemDictionaries? = dictionaries
		set(v)
		{
			field = v
			this.save()
		}

	var keyLayout: KeyLayout
		get() = this._keyLayout
			?: this.base?.keyLayout
			?: KeyLayout("")
		set(v) { this._keyLayout = v }
	var prefixStrokes: List<Stroke>
		get() = this._prefixStrokes
			?: this.base?.prefixStrokes
			?: listOf()
		set(v) { this._prefixStrokes = v }
	var suffixStrokes: List<Stroke>
		get() = this._suffixStrokes
			?: this.base?.suffixStrokes
			?: listOf()
		set(v) { this._suffixStrokes = v }

	val transforms: Map<
		CaseInsensitiveString,
		(FormattedText, UnformattedText, Boolean) -> UnformattedText>
		get() = this.manager.transforms
	val commands: Map<
		CaseInsensitiveString,
		(Translator, String) -> TranslationPart>
		get() = this.manager.commands
	var aliases: Map<CaseInsensitiveString, String>
		get() = this._aliases
			?: this.base?.aliases
			?: mapOf()
		set(v) { this._aliases = v }

	var defaultFormatting: Formatting
		get() = this._defaultFormatting
			?: this.base?.defaultFormatting
			?: Formatting()
		set(v) { this._defaultFormatting = v }

	var orthography: SystemOrthography
		get() = this._orthography
			?: this.base?.orthography
			?: SystemOrthography("", NULL_ORTHOGRAPHY)
		set(v) { this._orthography = v }

	var dictionaries: SystemDictionaries
		get() = this._dictionaries
			?: this.base?.dictionaries
			?: SystemDictionaries()
		set(v) { this._dictionaries = v }

	val machineConfig: MachineConfig = MachineConfig(this, machineConfig)

	fun save() = this.manager.saveSystem(this)

	companion object
}

fun System.copy(
	base: System? = this.base,

	path: String? = this.path,
	manager: SystemManager = this.manager,

	keyLayout: KeyLayout? = this.keyLayout,
	prefixStrokes: List<Stroke>? = this.prefixStrokes,
	suffixStrokes: List<Stroke>? = this.suffixStrokes,

	aliases: Map<CaseInsensitiveString, String>? = this.aliases,

	defaultFormatting: Formatting? = this.defaultFormatting,

	orthography: SystemOrthography? = this.orthography,

	dictionaries: SystemDictionaries? = this.dictionaries,

	machineConfig: JsonObject = this.machineConfig.json
) =
	System(
		base,

		path,
		manager,

		keyLayout,
		prefixStrokes,
		suffixStrokes,

		aliases,

		defaultFormatting,

		orthography,

		dictionaries,

		machineConfig
	)

fun System.toJson() = JsonObject().also({ system ->
	system.setNotNull("keyLayout", this._keyLayout?.toJson())
	system.setNotNull("aliases", this._aliases?.mapKeys({ it.key.value })?.toJson())
	system.setNotNull("prefixStrokes", this._prefixStrokes?.map({ it.rtfcre })?.toJson())
	system.setNotNull("suffixStrokes", this._suffixStrokes?.map({ it.rtfcre })?.toJson())
	system.setNotNull(
		"defaultFormatting",
		this._defaultFormatting
			?.let({ formatting ->
				JsonObject().also({
					it.set("space", formatting.space)
					it.set("spaceStart", formatting.spaceEnd.toString())
					it.set(
						"transform",
						this.manager
							.transforms
							.inverted()[formatting.transform]?.value)
					it.set(
						"singleTransform",
						this.manager
							.transforms
							.inverted()[formatting.singleTransform]?.value)
					it.set("transformState", formatting.transformState.toString())
				})
			})
	)
	system.setNotNull("orthography", this.orthography.path)
	system.setNotNull(
		"dictionaries",
		this._dictionaries?.dictionaries
			?.map({ d ->
				JsonObject().also({
					it.set("path", d.path)
					it.set("enabled", d.enabled)
				})
			})
			?.toJson()
	)
	system.setNotNull("machineConfig", this.machineConfig.json)
})

fun nimble.dotterel.translation.System.Companion.fromJson(
	manager: SystemManager,
	base: System?,
	json: JsonObject
): System
{
	val keyLayout = json.getOrNull("keyLayout")?.asObject()?.let({ KeyLayout(it) })
	val keyLayout2 = keyLayout ?: base?.keyLayout ?: NULL_SYSTEM.keyLayout

	return nimble.dotterel.translation.System(
		base = base,
		path = null,
		manager = manager,
		keyLayout = keyLayout,
		prefixStrokes = json.getOrNull("prefixStrokes")?.asArray()
			?.map({ keyLayout2.parse(it.asString()) }),
		suffixStrokes = json.getOrNull("suffixStrokes")?.asArray()
			?.map({ keyLayout2.parse(it.asString()) }),

		aliases = json.getOrNull("aliases")
			?.asObject()
			?.associateBy(
				{ CaseInsensitiveString(it.name) },
				{ it.value.asString() }),

		defaultFormatting = json.getOrNull("defaultFormatting")?.asObject()
			?.let({ formatting ->
				Formatting(
					space = formatting.getString("space", " "),
					spaceEnd = formatting.getOrNull("spaceStart")?.asString()
						?.let({ Formatting.Space.valueOf(it) })
						?: Formatting.Space.NORMAL,
					transform = manager.transforms[
						formatting.getOrNull("transform")
							?.asString()
							?.let({ CaseInsensitiveString(it) })],
					singleTransform = manager.transforms[
						formatting.getOrNull("singleTransform")
							?.asString()
							?.let({ CaseInsensitiveString(it) })],
					transformState = formatting.getOrNull("transformState")?.asString()
						?.let({ Formatting.TransformState.valueOf(it) })
						?: Formatting.TransformState.NORMAL)
			}),

		orthography = json.getOrNull("orthography")?.asString()
			?.let({ SystemOrthography(
				it,
				manager.openOrthography(it) ?: NULL_ORTHOGRAPHY)
			}),

		dictionaries = json.getOrNull("dictionaries")?.asArray()
			?.map({ it.asObject() })
			?.mapNotNull({
				manager.openDictionary(it.get("path").asString())
					?.let({ dictionary ->
						SystemDictionary(
							it.get("path").asString(),
							it.get("enabled").asBoolean(),
							dictionary)
					})
			})
			?.let({ SystemDictionaries(it) }),

		machineConfig = json.get("machineConfig")?.asObject() ?: JsonObject()
	)
}

val NULL_SYSTEM = System(
	base = null,
	path = null,
	manager = NULL_SYSTEM_MANAGER
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
			for(member in it)
				if(!member.value.isNull)
					acc.set(member.name, member.value)
			acc
		})
	}
	catch(e: ParseException)
	{
		return null
	}
}
