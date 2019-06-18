// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.PrettyPrint

import java.io.InputStream
import java.io.OutputStream

import nimble.dotterel.translation.*
import nimble.dotterel.util.toJson

open class ImmutableJsonDictionary(keyLayout: KeyLayout, json: JsonObject) :
	ImmutableBackedDictionary(
		keyLayout,
		BackingDictionary(
			json.map({ Pair(it.name, it.value.asString()) })
		))
{
	companion object
	{
		fun load(input: InputStream, keyLayout: KeyLayout): ImmutableJsonDictionary
		{
			try
			{
				return ImmutableJsonDictionary(
					keyLayout,
					Json.parse(input.bufferedReader()).asObject())
			}
			catch(e: com.eclipsesource.json.ParseException)
			{
				throw FileParseException("Invalid JSON", e)
			}
			catch(e: java.lang.NullPointerException)
			{
				throw FileParseException("Invalid type", e)
			}
			catch(e: java.lang.UnsupportedOperationException)
			{
				throw FileParseException("Missing type", e)
			}
		}
	}
}

class JsonDictionary(keyLayout: KeyLayout, json: JsonObject) :
	ImmutableJsonDictionary(keyLayout, json),
	SaveableDictionary
{
	override fun set(k: List<Stroke>, v: String)
	{
		this.backingDictionary[k.rtfcre] = v
	}
	override fun remove(k: List<Stroke>)
	{
		this.backingDictionary.remove(k.rtfcre)
	}

	override fun save(output: OutputStream) =
		output.bufferedWriter()
			.use({
				this.backingDictionary.entries
					.toSortedMap()
					.toJson()
					.writeTo(it, PrettyPrint.indentWithSpaces(0))
			})

	companion object
	{
		fun load(input: InputStream, keyLayout: KeyLayout): JsonDictionary
		{
			try
			{
				return JsonDictionary(keyLayout, Json.parse(input.bufferedReader()).asObject())
			}
			catch(e: com.eclipsesource.json.ParseException)
			{
				throw FileParseException("Invalid JSON", e)
			}
			catch(e: java.lang.NullPointerException)
			{
				throw FileParseException("Invalid type", e)
			}
			catch(e: java.lang.UnsupportedOperationException)
			{
				throw FileParseException("Missing type", e)
			}
		}
	}
}
