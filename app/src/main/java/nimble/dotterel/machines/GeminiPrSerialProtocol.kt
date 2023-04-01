// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.util.Log

import nimble.dotterel.util.toUnsignedInt

private const val BYTES_PER_STROKE = 6

class GeminiPrSerialProtocol(socket: SerialSocket) : StenoSerialProtocol(socket)
{
	override val keys = listOf(
		 "Fn",  "#1", "#2", "#3", "#4",   "#5",   "#6",
		"S1-", "S2-", "T-", "K-", "P-",   "W-",   "H-",
		 "R-",  "A-", "O-", "*1", "*2", "res1", "res2",
		"pwr",  "*3", "*4", "-E", "-U",   "-F",   "-R",
		 "-P",  "-B", "-L", "-G", "-T",   "-S",   "-D",
		 "#7",  "#8", "#9", "#A", "#B",   "#C",   "-Z"
	)
		.chunked(7)
		.map({ row -> row.reversed() })
		.flatten()

	private val buffer = mutableListOf<Int>()

	private fun processBuffer()
	{
		val keys = this.buffer.withIndex().fold(
			0L,
			{ acc, it ->
				acc or ((it.value.toLong() and 0b1111111) shl (it.index * 7))
			})
		this.applyStroke(keys)

		this.buffer.clear()
	}

	override fun receive(data: ByteArray)
	{
		for(b in data)
		{
			val ub = b.toUnsignedInt()
			// First byte of chord
			if(ub and 0b10000000 != 0 && this.buffer.isNotEmpty())
			{
				val hexData = this.buffer.joinToString(
					separator = " ",
					transform = { String.format("%02X", it)})
				Log.w("Dotterel GeminiPr", "discarding invalid packet: $hexData")
				this.buffer.clear()
			}

			this.buffer.add(ub)
			if(this.buffer.size == BYTES_PER_STROKE)
				this.processBuffer()
		}
	}
}
