// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

class KeyMap<T>(
	val layout: KeyLayout,
	map: Map<String, List<String>>,
	transform: (String) -> T?)
{
	val map: Map<T, Stroke> = map.let({
		val invertedMap = mutableMapOf<T, Stroke>()
		for(mapping in it)
			for(key in mapping.value)
			{
				val key2 = transform(key) ?: continue
				invertedMap[key2] = this.layout.parse(mapping.key)
			}
		invertedMap
	})

	fun isMapped(key: T) = this.map.contains(key)
	fun parse(key: T): Stroke = this.map[key]?.copy() ?: Stroke(this.layout, 0L)
}
