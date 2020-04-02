// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import com.eclipsesource.json.Json

import java.io.InputStream

import nimble.dotterel.translation.FileParseException

class JsonDictionary(input: InputStream) : StandardDictionary()
{
	init
	{
		try
		{
			for(entry in Json.parse(input.bufferedReader()).asObject())
				this[entry.name] = entry.value.asString()
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
