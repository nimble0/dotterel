// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.matchers.gt
import io.kotlintest.specs.FunSpec

import java.io.*

import nimble.dotterel.translation.*
import nimble.dotterel.util.CaseInsensitiveString
import nimble.dotterel.util.getOrNull
import nimble.dotterel.util.toJson

class LocalSystemResources : SystemResources
{
	override val transforms = TRANSFORMS
	override val commands = COMMANDS
	override val codeDictionaries = mapOf<String, Dictionary>()

	override fun openInputStream(path: String): InputStream?
	{
		try
		{
			val type = path.substringBefore(":")
			val name = path.substringAfter(":/")
			return when(type)
			{
				"asset" -> File("src/main/assets/$name").inputStream()
				else -> File(path).inputStream()
			}
		}
		catch(e: FileNotFoundException)
		{
			println("Dotterel: File $path not found")
		}
		catch(e: IOException)
		{
			println("Dotterel: IO Exception: ${e.message}")
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
				"asset" ->
				{
					println("Dotterel: $path is read-only")
					null
				}
				else -> File(path).outputStream()
			}
		}
		catch(e: FileNotFoundException)
		{
			println("Dotterel: File $path not found")
		}
		catch(e: IOException)
		{
			println("Dotterel: IO Exception: ${e.message}")
		}

		return null
	}
}

class SystemTests : FunSpec
({
	val systemManager = SystemManager(LocalSystemResources(), { println(it) })

	test("empty system")
	{
		val system = System(null, null, NULL_SYSTEM_MANAGER)

		system.keyLayout shouldBe KeyLayout("")
		system.prefixStrokes.size shouldBe 0
		system.suffixStrokes.size shouldBe 0
		system.aliases.size shouldBe 0
		system.defaultFormatting shouldBe Formatting()
		system.orthographies.orthographies.size shouldBe 0
		system.dictionaries.dictionaries.size shouldBe 0
		system.machineConfig["NKRO"] shouldBe null
		system.machineConfig["On Screen"] shouldBe null
	}

	test("system saving/loading")
	{
		val system = systemManager.openSystem("asset:/systems/ireland.english.json")!!

		system.keyLayout.pureKeysString shouldBe "#STKPWHR-AO*EU-FRPBLGTSDZ"
		system.prefixStrokes shouldBe listOf<Stroke>()
		system.suffixStrokes shouldBe system.keyLayout.parse(listOf("-Z", "-D", "-S", "-G"))
		system.aliases[CaseInsensitiveString(".")] shouldNotBe null
		system.aliases[CaseInsensitiveString(",")] shouldNotBe null
		system.aliases[CaseInsensitiveString("?")] shouldNotBe null
		system.aliases[CaseInsensitiveString("!")] shouldNotBe null
		system.defaultFormatting.space shouldBe " "
		system.orthographies.orthographies.size shouldBe gt(0)
		system.dictionaries.dictionaries.size shouldBe gt(0)
		system.machineConfig["NKRO"] shouldNotBe null
		system.machineConfig["On Screen"] shouldNotBe null

		val systemJson = system.toJson()
		val system2 = System.fromJson(systemManager, null, systemJson)

		system.keyLayout shouldBe system2.keyLayout
		system.prefixStrokes shouldBe system2.prefixStrokes
		system.suffixStrokes shouldBe system2.suffixStrokes
		system.aliases shouldBe system2.aliases
		system.defaultFormatting shouldBe system2.defaultFormatting
		(system.orthographies.orthographies.map({ Pair(it.enabled, it.path) })
			shouldBe system2.orthographies.orthographies.map({ Pair(it.enabled, it.path) }))
		(system.dictionaries.dictionaries.map({ Pair(it.enabled, it.path) })
			shouldBe system2.dictionaries.dictionaries.map({ Pair(it.enabled, it.path) }))
		system.machineConfig shouldBe system2.machineConfig
	}

	test("system inheritance")
	{
		val base = systemManager.openSystem("asset:/systems/ireland.english.json")!!

		val system1 = System.fromJson(systemManager, base.toJson(), JsonObject())

		system1.keyLayout shouldBe base.keyLayout
		system1.prefixStrokes shouldBe base.prefixStrokes
		system1.suffixStrokes shouldBe base.suffixStrokes
		system1.aliases shouldBe base.aliases
		system1.defaultFormatting shouldBe base.defaultFormatting
		system1.orthographies shouldBe base.orthographies
		system1.dictionaries shouldBe base.dictionaries
		system1.machineConfig["NKRO"] shouldNotBe null
		system1.machineConfig["On Screen"] shouldNotBe null
		system1.machineConfig["TX Bolt"] shouldBe null

		val keyLayout = KeyLayout("ABCDE-FG-HIJKLMNOP")
		val prefixStrokes = keyLayout.parse(listOf("A-", "C-"))
		val suffixStrokes = keyLayout.parse(listOf("-L"))
		val aliases = base.aliases + mapOf(Pair(CaseInsensitiveString("abc"), "cdf"))
		val system2 = System.fromJson(
			systemManager,
			base.toJson(),
			JsonObject().also({ json ->
				json.set("keyLayout", keyLayout.toJson())
				json.set("prefixStrokes", prefixStrokes.map({ it.rtfcre }).toJson())
				json.set("suffixStrokes", suffixStrokes.map({ it.rtfcre }).toJson())
				json.set("aliases", aliases.mapKeys({ it.key.value }).toJson())
			}))

		system2.keyLayout shouldBe keyLayout
		system2.prefixStrokes shouldBe prefixStrokes
		system2.suffixStrokes shouldBe suffixStrokes
		system2.aliases shouldBe aliases
		system2.defaultFormatting shouldBe base.defaultFormatting
		system2.orthographies shouldBe base.orthographies
		(system2.dictionaries.dictionaries.map({ Pair(it.enabled, it.path) })
			shouldBe base.dictionaries.dictionaries.map({ Pair(it.enabled, it.path) }))
		system2.machineConfig["NKRO"] shouldNotBe null
		system2.machineConfig["On Screen"] shouldNotBe null
		system2.machineConfig["TX Bolt"] shouldBe null

		val defaultFormatting = Formatting(
			space = "-",
			spaceEnd = Formatting.Space.NONE,
			singleTransform = systemManager.transforms[CaseInsensitiveString("CAPITALISE")],
			transformState = Formatting.TransformState.MAIN
		)
		val dictionaries = SystemDictionaries(
			base.dictionaries.dictionaries.subList(
				0,
				base.dictionaries.dictionaries.size - 1)
		)
		val orthographies = SystemOrthographies(listOf())
		val machineConfig = JsonObject().also({
			it.set("On Screen", Json.NULL)
			it.set("TX Bolt", JsonObject())
		})
		val system3 = System.fromJson(
			systemManager,
			base.toJson(),
			JsonObject().also({ json ->
				json.set("orthographies", orthographies.orthographies
					.map({ d ->
						JsonObject().also({
							it.set("path", d.path)
							it.set("enabled", d.enabled)
						})
					})
					.toJson())
				json.set("dictionaries", dictionaries.toJson())
				json.set("defaultFormatting", defaultFormattingToJson(systemManager, defaultFormatting))
				json.set("machineConfig", machineConfig)
			}))

		system3.keyLayout shouldBe base.keyLayout
		system3.prefixStrokes shouldBe base.prefixStrokes
		system3.suffixStrokes shouldBe base.suffixStrokes
		system3.aliases shouldBe base.aliases
		system3.defaultFormatting shouldBe defaultFormatting
		system3.orthographies shouldBe orthographies
		system3.dictionaries shouldBe dictionaries
		system3.machineConfig["NKRO"] shouldNotBe null
		system3.machineConfig["On Screen"] shouldNotBe null
		system3.machineConfig["TX Bolt"] shouldBe JsonObject()
	}
})
