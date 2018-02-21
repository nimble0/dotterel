// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import android.util.JsonReader

import java.io.InputStream
import java.io.InputStreamReader

class JsonDictionary(input: InputStream) : StandardDictionary()
{
	init
	{
		JsonReader(InputStreamReader(input, "UTF-8")).use({
			it.beginObject()
			while(it.hasNext())
				this[it.nextName()] = it.nextString()
			it.endObject()
		})
	}
}
