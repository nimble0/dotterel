// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.orthographies

import io.kotlintest.matchers.shouldBe

import nimble.dotterel.translation.Orthography
import nimble.dotterel.translation.apply

fun Orthography.testUnmatchedInvariance()
{
	// It is not required that these preceding texts are unmatched by orthography rules.
	// Callers should ensure that this is the case.
	val precedingTexts = listOf(
		"and the ",
		"end.",
		"was,",
	)

	val words = listOf(
		"narrate",
		"bug",
		"response",
		"hut",
		"hug",
		"nation",
		"ton",
		"ration",
		"yon",
		"rat",
		"ratt",
		"tian",
		"glub",
		"Nation",
	)

	val suffixes = listOf(
		"ing",
		"ed",
		"er",
		"y",
		"ted",
		"ing",
	)

	words.forEach({ word ->
		suffixes.forEach({ suffix ->
			val result = this.apply(word, suffix)
			precedingTexts.forEach({ preceding ->
				val result2 = this.apply(preceding + word, suffix)
				if(result == null)
					result2 shouldBe null
				else
					result2 shouldBe (preceding + result)
			})
		})
	})
}
