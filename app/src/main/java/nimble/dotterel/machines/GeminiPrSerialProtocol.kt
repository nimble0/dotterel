// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

private const val BYTES_PER_STROKE = 6

class GeminiPrSerialProtocol(socket: SerialSocket) : StenoSerialProtocol(socket)
{
	override val keys = listOf(
		 "Fn",  "#1", "#2", "#3", "#4",   "#5",   "#6",
		"S1-", "S2-", "T-", "K-", "P-",   "W-",   "H-",
		 "R-",  "A-", "O-", "*1", "*2", "res1", "res2",
		 "pwr", "*3", "*4", "-E", "-U",   "-F",   "-R",
		 "-P",  "-B", "-L", "-G", "-T",   "-S",   "-D",
		 "#7",  "#8", "#9", "#A", "#B",   "#C",   "-Z"
	)

	override fun receive(data: ByteArray)
	{
		if(data.size != 6
			|| (data[0].toUnsignedInt() and 0b1000000 == 0)
			|| data.drop(1).any({ (data[0].toUnsignedInt() and 0b1000000 != 0) }))
		{
			val hexData = data.joinToString(
				separator = " ",
				transform = { String.format("%02X", it)})
			val error = "discarding invalid packet: $hexData"
			return
		}

		val keys = data.withIndex().fold(
			0L,
			{ acc, it ->
				acc or (it.value.toLong() and 0b1111111) shl (it.index * 7)
			})
		this.applyStroke(keys)
	}
}
