// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.*
import io.kotlintest.specs.FunSpec

import nimble.dotterel.translation.*

class FormattingTests : FunSpec
({
	test("spacing")
	{
		val spacingTable = table(
			headers("context", "text", "result"),
			row(Formatting.Space.NORMAL, Formatting.Space.NORMAL, " "),
			row(Formatting.Space.NORMAL, Formatting.Space.NONE, ""),
			row(Formatting.Space.NORMAL, Formatting.Space.GLUE, " "),
			row(Formatting.Space.NONE, Formatting.Space.NORMAL, ""),
			row(Formatting.Space.NONE, Formatting.Space.NONE, ""),
			row(Formatting.Space.NONE, Formatting.Space.GLUE, ""),
			row(Formatting.Space.GLUE, Formatting.Space.NORMAL, " "),
			row(Formatting.Space.GLUE, Formatting.Space.NONE, ""),
			row(Formatting.Space.GLUE, Formatting.Space.GLUE, "")
		)
		forAll(spacingTable) { a, b, result ->
			UnformattedText(0, "text", Formatting(spaceStart = b))
				.format(FormattedText(0, "context", Formatting(space = " ", spaceEnd = a)))
				.text shouldBe result + "text"
		}

		run {
			val context = FormattedText(0, "context", Formatting(space = " "))
			val a = UnformattedText(0, "text").format(context)
			val b = UnformattedText(0, "btextb").format(a)

			b.text shouldBe " btextb"
		}

		run {
			val context = FormattedText(
				0,
				"context",
				Formatting(space = " ", spaceEnd = Formatting.Space.NORMAL))
			val a = UnformattedText(
				0,
				"text",
				Formatting(spaceStart = Formatting.Space.NONE))
				.format(context)
			val b = UnformattedText(
				0,
				"btextb",
				Formatting(spaceStart = Formatting.Space.NORMAL))
				.format(a)

			b.text shouldBe " btextb"
		}

		run {
			val context = FormattedText(
				0,
				"context",
				Formatting(space = " ", spaceEnd = Formatting.Space.NORMAL))
			val a = UnformattedText(
				0,
				"text",
				Formatting(
					spaceStart = Formatting.Space.NONE,
					spaceEnd = Formatting.Space.NONE)
			).format(context)
			val b = UnformattedText(
				0,
				"btextb",
				Formatting(spaceStart = Formatting.Space.NORMAL)
			).format(a)

			b.text shouldBe "btextb"
		}
	}

	test("custom space")
	{
		run {
			val context = FormattedText(
				0,
				"context",
				Formatting(space = "_@", spaceEnd = Formatting.Space.NORMAL))
			val a = UnformattedText(
				0,
				"text  text",
				Formatting(spaceStart = Formatting.Space.NORMAL)
			).format(context)

			a.text shouldBe "_@text_@_@text"
		}

		run {
			val context = FormattedText(
				0,
				"context",
				Formatting(space = "_@", spaceEnd = Formatting.Space.GLUE))
			val a = UnformattedText(
				0,
				"text  text",
				Formatting(spaceStart = Formatting.Space.GLUE)
			).format(context)

			a.text shouldBe "text_@_@text"
		}
	}

	test("backspacing")
	{
		(FormattedText(10, "context") + FormattedText(6, "text"))
			.text shouldBe "ctext"

		(FormattedText(10, "context") + FormattedText(12, "text"))
			.text shouldBe "text"

		(FormattedText(10, "context") + FormattedText(12, "text"))
			.backspaces shouldBe 15
	}

	test("orthography")
	{
		val orthography = RegexOrthography(mutableListOf(
			RegexOrthography.Replacement(Regex("(?<=[bcdfghjklmnpqrstvwxz] ?)y\uffffs"), "ies"),
			RegexOrthography.Replacement(Regex("(?<=s|sh|x|z|zh ?)\uffffs"), "es"),
			RegexOrthography.Replacement(Regex("ie\uffffing"), "ying")
		))

		run {
			val context = FormattedText(
				0,
				"context",
				Formatting(orthography = orthography))
			val a = UnformattedText(
				0,
				"ing",
				Formatting(spaceStart = Formatting.Space.NONE)
			).format(context)

			a.backspaces shouldBe 0
			a.text shouldBe "ing"
		}

		run {
			val context = FormattedText(
				0,
				"deny",
				Formatting(orthography = orthography))
			val a = UnformattedText(
				0,
				"s",
				Formatting(spaceStart = Formatting.Space.NONE)
			).format(context)

			a.backspaces shouldBe 1
			a.text shouldBe "ies"
		}

		run {
			val context = FormattedText(
				0,
				"lie",
				Formatting(orthography = orthography))
			val a = UnformattedText(
				0,
				"ing",
				Formatting(spaceStart = Formatting.Space.NONE)
			).format(context)

			a.backspaces shouldBe 2
			a.text shouldBe "ying"
		}

		run {
			val context = FormattedText(
				0,
				"brush",
				Formatting(orthography = orthography))
			val a = UnformattedText(
				0,
				"s",
				Formatting(spaceStart = Formatting.Space.NONE)
			).format(context)

			a.backspaces shouldBe 0
			a.text shouldBe "es"
		}

		run {
			val context = FormattedText(
				0,
				"deny",
				Formatting(orthography = orthography, orthographyStart = false))
			val a = UnformattedText(
				0,
				"s",
				Formatting(spaceStart = Formatting.Space.NONE, orthographyEnd = false)
			).format(context)

			a.backspaces shouldBe 1
			a.text shouldBe "ies"
		}

		run {
			val context = FormattedText(
				0,
				"deny",
				Formatting(orthography = orthography, orthographyEnd = false))
			val a = UnformattedText(
				0,
				"s",
				Formatting(spaceStart = Formatting.Space.NONE)
			).format(context)

			a.backspaces shouldBe 0
			a.text shouldBe "s"
		}

		run {
			val context = FormattedText(
				0,
				"deny",
				Formatting(orthography = orthography))
			val a = UnformattedText(
				0,
				"s",
				Formatting(spaceStart = Formatting.Space.NONE, orthographyStart = false)
			).format(context)

			a.backspaces shouldBe 0
			a.text shouldBe "s"
		}

		run {
			val orthography2 = RegexOrthography(mutableListOf(
				RegexOrthography.Replacement(
					Regex("(?<=[bcdfghjklmnpqrstvwxz] ?)y\uffffs"),
					"ies")))

			val context = FormattedText(
				0,
				"deny",
				Formatting(orthography = orthography))
			val a = UnformattedText(
				0,
				"s",
				Formatting(
					spaceStart = Formatting.Space.NONE,
					orthography = orthography2,
					orthographyStart = false)
			).format(context)

			a.backspaces shouldBe 0
			a.text shouldBe "s"
		}
	}
})
