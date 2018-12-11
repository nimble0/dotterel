// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.JsonObject

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

class System(
	val path: String?,
	val manager: SystemManager,

	keyLayout: KeyLayout,
	prefixStrokes: List<Stroke>,
	suffixStrokes: List<Stroke>,

	aliases: Map<CaseInsensitiveString, String>,

	defaultFormatting: Formatting,

	orthography: SystemOrthography,

	dictionaries: SystemDictionaries)
{
	var keyLayout: KeyLayout = keyLayout
		set(v)
		{
			field = v
			this.save()
		}
	var prefixStrokes: List<Stroke> = prefixStrokes
		set(v)
		{
			field = v
			this.save()
		}
	var suffixStrokes: List<Stroke> = suffixStrokes
		set(v)
		{
			field = v
			this.save()
		}

	val transforms: Map<
		CaseInsensitiveString,
		(FormattedText, UnformattedText, Boolean) -> UnformattedText>
		get() = this.manager.transforms
	val commands: Map<
		CaseInsensitiveString,
		(Translator, String) -> TranslationPart>
		get() = this.manager.commands
	var aliases: Map<CaseInsensitiveString, String> = aliases
		set(v)
		{
			field = v
			this.save()
		}

	var dictionaries: SystemDictionaries = dictionaries
		set(v)
		{
			field = v
			this.save()
		}
	var orthography: SystemOrthography = orthography
		set(v)
		{
			field = v
			this.save()
		}

	var defaultFormatting: Formatting = defaultFormatting
		set(v)
		{
			field = v
			this.save()
		}

	fun save() = this.manager.saveSystem(this)

	companion object
}

fun System.copy(
	path: String? = this.path,
	manager: SystemManager = this.manager,

	keyLayout: KeyLayout = this.keyLayout,
	prefixStrokes: List<Stroke> = this.prefixStrokes,
	suffixStrokes: List<Stroke> = this.suffixStrokes,

	aliases: Map<CaseInsensitiveString, String> = this.aliases,

	defaultFormatting: Formatting = this.defaultFormatting,

	orthography: SystemOrthography = this.orthography,

	dictionaries: SystemDictionaries = this.dictionaries
) =
	System(
		path,
		manager,

		keyLayout,
		prefixStrokes,
		suffixStrokes,

		aliases,

		defaultFormatting,

		orthography,

		dictionaries
	)

fun System.toJson() = JsonObject().also({ system ->
	system.set("keyLayout", this.keyLayout.toJson())
	system.set("aliases", this.aliases.mapKeys({ it.key.value }).toJson())
	system.set("prefixStrokes", this.prefixStrokes.map({ it.rtfcre }).toJson())
	system.set("suffixStrokes", this.suffixStrokes.map({ it.rtfcre }).toJson())
	system.set(
		"defaultFormatting",
		this.defaultFormatting
			.let({ formatting ->
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
					it.set("transformState", formatting.toString())
				})
			})
	)
	system.set("orthography", this.orthography.path)
	system.set(
		"dictionaries",
		this.dictionaries.dictionaries
			.map({ d ->
				JsonObject().also({
					it.set("path", d.path)
					it.set("enabled", d.enabled)
				})
			})
			.toJson()
	)
})

fun nimble.dotterel.translation.System.Companion.fromJson(
	manager: SystemManager,
	json: JsonObject
): System
{
	val keyLayout = json.get("keyLayout").asObject().let({ KeyLayout(it) })

	return nimble.dotterel.translation.System(
		path = null,
		manager = manager,
		keyLayout = keyLayout,
		prefixStrokes = json.get("prefixStrokes").asArray()
			.map({ keyLayout.parse(it.asString()) }),
		suffixStrokes = json.get("suffixStrokes").asArray()
			.map({ keyLayout.parse(it.asString()) }),

		aliases = json.get("aliases")
			.asObject()
			.associateBy(
				{ CaseInsensitiveString(it.name) },
				{ it.value.asString() }),

		defaultFormatting = json.get("defaultFormatting").asObject()
			.let({ formatting ->
				Formatting(
					space = formatting.getString("space", " "),
					spaceEnd = formatting.getString("spaceStart", null)
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
					transformState = formatting.getString("transformState", null)
						?.let({ Formatting.TransformState.valueOf(it) })
						?: Formatting.TransformState.NORMAL)
			}),

		orthography = json.getString("orthography", null)
			.let({ SystemOrthography(
				it,
				manager.openOrthography(it) ?: NULL_ORTHOGRAPHY)
			}),

		dictionaries = json.get("dictionaries").asArray()
			.map({ it.asObject() })
			.mapNotNull({
				manager.openDictionary(it.get("path").asString())
					?.let({ dictionary ->
						SystemDictionary(
							it.get("path").asString(),
							it.get("enabled").asBoolean(),
							dictionary)
					})
			})
			.let({ SystemDictionaries(it) })
	)
}

val NULL_SYSTEM = System(
	path = null,
	manager = NULL_SYSTEM_MANAGER,

	keyLayout = KeyLayout(""),
	prefixStrokes = listOf(),
	suffixStrokes = listOf(),

	aliases = mapOf(),

	defaultFormatting = Formatting(),

	orthography = SystemOrthography("code://orthography/null", NULL_ORTHOGRAPHY),

	dictionaries = SystemDictionaries()
)
