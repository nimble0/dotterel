// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.*

private fun Orthography.apply2(left: String, right: String): String =
	this.apply(left, right) ?: left + right

class EnglishOrthographyTests : FunSpec
({
	val systemManager = SystemManager(LocalSystemResources(), { println(it) })

	val orthography = SystemOrthographies(listOf(
			"asset://orthography/english.simple.json",
			"asset://orthography/english.regex.json"
		).map({ SystemOrthography(
			it,
			true,
			systemManager.openOrthography(it)!!)
		})
	)

	orthography.apply2("artistic", "ly") shouldBe "artistically"
	orthography.apply2("cosmetic", "ly") shouldBe "cosmetically"
	orthography.apply2("establish", "s") shouldBe "establishes"
	orthography.apply2("speech", "s") shouldBe "speeches"
	orthography.apply2("approach", "s") shouldBe "approaches"
	orthography.apply2("beach", "s") shouldBe "beaches"
	orthography.apply2("arch", "s") shouldBe "arches"
	orthography.apply2("larch", "s") shouldBe "larches"
	orthography.apply2("march", "s") shouldBe "marches"
	orthography.apply2("search", "s") shouldBe "searches"
	orthography.apply2("starch", "s") shouldBe "starches"
	orthography.apply2("stomach", "s") shouldBe "stomachs"
	orthography.apply2("monarch", "s") shouldBe "monarchs"
	orthography.apply2("patriarch", "s") shouldBe "patriarchs"
	orthography.apply2("oligarch", "s") shouldBe "oligarchs"
	orthography.apply2("cherry", "s") shouldBe "cherries"
	orthography.apply2("day", "s") shouldBe "days"
	orthography.apply2("penny", "s") shouldBe "pennies"
	orthography.apply2("pharmacy", "ist") shouldBe "pharmacist"
	orthography.apply2("melody", "ist") shouldBe "melodist"
	orthography.apply2("pacify", "ist") shouldBe "pacifist"
	orthography.apply2("geology", "ist") shouldBe "geologist"
	orthography.apply2("metallurgy", "ist") shouldBe "metallurgist"
	orthography.apply2("anarchy", "ist") shouldBe "anarchist"
	orthography.apply2("monopoly", "ist") shouldBe "monopolist"
	orthography.apply2("alchemy", "ist") shouldBe "alchemist"
	orthography.apply2("botany", "ist") shouldBe "botanist"
	orthography.apply2("therapy", "ist") shouldBe "therapist"
	orthography.apply2("theory", "ist") shouldBe "theorist"
	orthography.apply2("psychiatry", "ist") shouldBe "psychiatrist"
	orthography.apply2("lobby", "ist") shouldBe "lobbyist"
	orthography.apply2("hobby", "ist") shouldBe "hobbyist"
	orthography.apply2("copy", "ist") shouldBe "copyist"
	orthography.apply2("beauty", "ful") shouldBe "beautiful"
	orthography.apply2("weary", "ness") shouldBe "weariness"
	orthography.apply2("weary", "some") shouldBe "wearisome"
	orthography.apply2("lonely", "ness") shouldBe "loneliness"
	orthography.apply2("narrate", "ing") shouldBe "narrating"
	orthography.apply2("narrate", "or") shouldBe "narrator"
	orthography.apply2("generalize", "ability") shouldBe "generalizability"
	orthography.apply2("reproduce", "able") shouldBe "reproducible"
	orthography.apply2("grade", "ations") shouldBe "gradations"
	orthography.apply2("urine", "ary") shouldBe "urinary"
	orthography.apply2("achieve", "able") shouldBe "achievable"
	orthography.apply2("polarize", "ation") shouldBe "polarization"
	orthography.apply2("done", "or") shouldBe "donor"
	orthography.apply2("analyze", "ed") shouldBe "analyzed"
	orthography.apply2("narrate", "ing") shouldBe "narrating"
	orthography.apply2("believe", "able") shouldBe "believable"
	orthography.apply2("animate", "ors") shouldBe "animators"
	orthography.apply2("discontinue", "ation") shouldBe "discontinuation"
	orthography.apply2("innovate", "ive") shouldBe "innovative"
	orthography.apply2("future", "ists") shouldBe "futurists"
	orthography.apply2("illustrate", "or") shouldBe "illustrator"
	orthography.apply2("emerge", "ent") shouldBe "emergent"
	orthography.apply2("equip", "ed") shouldBe "equipped"
	orthography.apply2("defer", "ed") shouldBe "deferred"
	orthography.apply2("defer", "er") shouldBe "deferrer"
	orthography.apply2("defer", "ing") shouldBe "deferring"
	orthography.apply2("pigment", "ed") shouldBe "pigmented"
	orthography.apply2("refer", "ed") shouldBe "referred"
	orthography.apply2("fix", "ed") shouldBe "fixed"
	orthography.apply2("alter", "ed") shouldBe "altered"
	orthography.apply2("interpret", "ing") shouldBe "interpreting"
	orthography.apply2("wonder", "ing") shouldBe "wondering"
	orthography.apply2("target", "ing") shouldBe "targeting"
	orthography.apply2("limit", "er") shouldBe "limiter"
	orthography.apply2("maneuver", "ing") shouldBe "maneuvering"
	orthography.apply2("monitor", "ing") shouldBe "monitoring"
	orthography.apply2("color", "ing") shouldBe "coloring"
	orthography.apply2("inhibit", "ing") shouldBe "inhibiting"
	orthography.apply2("master", "ed") shouldBe "mastered"
	orthography.apply2("target", "ing") shouldBe "targeting"
	orthography.apply2("fix", "ed") shouldBe "fixed"
	orthography.apply2("scrap", "y") shouldBe "scrappy"
	orthography.apply2("trip", "s") shouldBe "trips"
	orthography.apply2("equip", "s") shouldBe "equips"
	orthography.apply2("bat", "en") shouldBe "batten"
	orthography.apply2("smite", "en") shouldBe "smitten"
	orthography.apply2("got", "en") shouldBe "gotten"
	orthography.apply2("bite", "en") shouldBe "bitten"
	orthography.apply2("write", "en") shouldBe "written"
	orthography.apply2("flax", "en") shouldBe "flaxen"
	orthography.apply2("wax", "en") shouldBe "waxen"
	orthography.apply2("fast", "est") shouldBe "fastest"
	orthography.apply2("white", "er") shouldBe "whiter"
	orthography.apply2("crap", "y") shouldBe "crappy"
	orthography.apply2("lad", "er") shouldBe "ladder"
	orthography.apply2("translucent", "cy") shouldBe "translucency"
	orthography.apply2("bankrupt", "cy") shouldBe "bankruptcy"
	orthography.apply2("inadequate", "cy") shouldBe "inadequacy"
	orthography.apply2("secret", "cy") shouldBe "secrecy"
	orthography.apply2("impolite", "cy") shouldBe "impolicy"
	orthography.apply2("idiot", "cy") shouldBe "idiocy"
	orthography.apply2("translucent", "cies") shouldBe "translucencies"
	orthography.apply2("bankrupt", "cies") shouldBe "bankruptcies"
	orthography.apply2("inadequate", "cies") shouldBe "inadequacies"
	orthography.apply2("secret", "cies") shouldBe "secrecies"
	orthography.apply2("impolite", "cies") shouldBe "impolicies"
	orthography.apply2("idiot", "cies") shouldBe "idiocies"
	orthography.apply2("free", "ed") shouldBe "freed"
	orthography.apply2("free", "er") shouldBe "freer"
	orthography.apply2("regulate", "ry") shouldBe "regulatory"
	orthography.apply2("star", "y") shouldBe "starry"
	orthography.apply2("star", "ed") shouldBe "starred"
})
