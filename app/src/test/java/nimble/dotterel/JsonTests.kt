// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.FunSpec

import java.lang.UnsupportedOperationException

import nimble.dotterel.util.get
import nimble.dotterel.util.set

class JsonTests : FunSpec
({
	val json = Json.parse("""{
				"a": "A",
				"b": [1, 2, 3],
				"c": {},
				"d": {
					"a": "A",
					"b": {}
				}
			}""".trimMargin())
		.asObject()

	test("get/set path")
	{
		json.get(listOf("a"))?.asString() shouldBe "A"
		shouldThrow<UnsupportedOperationException>
		{
			json.get(listOf("b", "a"))
		}
		json.get(listOf("c", "a")) shouldBe null
		json.get(listOf("d", "a"))?.asString() shouldBe "A"
		json.get(listOf("d", "b"))?.asObject() shouldBe JsonObject()
		json.get(listOf("d", "b", "a")) shouldBe null

		json.get(listOf<String>()) shouldBe json
		json.get("a").get(listOf())?.asString() shouldBe "A"
		shouldThrow<UnsupportedOperationException>
		{
			json.get("a").get(listOf("b"))
		}


		json.set(listOf("a"), "a")
		json.get(listOf("a"))?.asString() shouldBe "a"
		shouldThrow<UnsupportedOperationException>
		{
			json.set(listOf("b", "a"), "b")
		}
		json.set(listOf("c", "a", "b"), "C")
		json.get(listOf("c", "a", "b"))?.asString() shouldBe "C"
		json.set(listOf("d", "b"), "D")
		json.get(listOf("d", "b"))?.asString() shouldBe "D"

		shouldThrow<IndexOutOfBoundsException>
		{
			json.set(listOf<String>(), "a")
		}
		shouldThrow<UnsupportedOperationException>
		{
			json.get("a").set(listOf("b"), "B")
		}
	}
})
