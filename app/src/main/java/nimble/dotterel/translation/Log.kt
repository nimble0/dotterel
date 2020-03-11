// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

interface Log
{
	fun info(message: String)
	{
		println("Info: $message")
	}
	fun error(message: String)
	{
		println("Error: $message")
	}
}
