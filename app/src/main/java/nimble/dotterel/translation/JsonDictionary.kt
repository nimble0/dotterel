// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.Json

import java.io.InputStream

class JsonDictionary(input: InputStream) : StandardDictionary()
{
	init
	{
		for(entry in Json.parse(input.bufferedReader()).asObject())
			this[entry.name] = entry.value.asString()
	}
}
