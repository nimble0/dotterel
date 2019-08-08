// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

class TxBoltSerialProtocol(socket: SerialSocket) : StenoSerialProtocol(socket)
{
	override val keys = listOf(
		"S-", "T-", "K-", "P-", "W-", "H-", "R-",
		"A-", "O-",  "*", "-E", "-U",
		"-F", "-R", "-P", "-B", "-L", "-G", "-T", "-S", "-D", "-Z",
		"#"
	)

	private var lastKeySet = 0
	private var currentKeys = 0L

	private fun applyStroke()
	{
		if(this.currentKeys != 0L)
		{
			this.applyStroke(this.currentKeys)

			this.lastKeySet = 0
			this.currentKeys = 0L
		}
	}

	override fun receive(data: ByteArray)
	{
		for(b in data)
		{
			val keySet = b.toUnsignedInt() shr 6
			if(keySet < this.lastKeySet)
				this.applyStroke()

			this.lastKeySet = keySet
			val keys = (b.toLong() and 0b111111) shl (keySet * 6)
			this.currentKeys = this.currentKeys or keys

			if(this.lastKeySet == 3 || b.toInt() == 0)
				this.applyStroke()
		}
	}
}

