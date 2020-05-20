// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject

import java.io.InputStream
import java.util.Locale

import kotlin.math.*

import nimble.dotterel.util.toJson

internal fun String.withCase(s: String): String =
	(this.zip(s, transform = { a, b ->
		if(b.isUpperCase())
			a.toUpperCase()
		else
			a
	}).joinToString("")
		+ this.substring(min(s.length, this.length)))

class SimpleOrthography : Orthography
{
	internal data class Replacement(
		val left: String,
		val right: String,
		var replacement: String) : Comparable<Replacement>
	{
		override fun compareTo(other: Replacement): Int =
			(this.left.reversed().compareTo(other.left.reversed()) * 2
				+ min(max(this.right.compareTo(other.right), -1), 1))
	}

	internal data class LeftLookupEntry(
		val left: String,
		var parent: LeftLookupEntry? = null,
		val rights: MutableList<RightLookupEntry> = mutableListOf()
	) : Comparable<LeftLookupEntry>
	{
		override fun compareTo(other: LeftLookupEntry): Int =
			this.left.reversed().compareTo(other.left.reversed())
	}

	internal data class RightLookupEntry(
		val right: String,
		val replacement: String,
		var parent: RightLookupEntry? = null
	) : Comparable<RightLookupEntry>
	{
		override fun compareTo(other: RightLookupEntry): Int =
			this.right.compareTo(other.right)
	}

	internal val replacements = mutableListOf<LeftLookupEntry>()

	internal fun find(left: String, right: String): Replacement?
	{
		val leftMatchI = this.replacements.binarySearch(LeftLookupEntry(left))
		if(leftMatchI == -1)
			return null

		var leftMatch = if(leftMatchI >= 0)
				this.replacements[leftMatchI]
			else
			{
				var leftMatch = this.replacements[(-leftMatchI - 1) - 1]
				while(!left.endsWith(leftMatch.left))
					leftMatch = leftMatch.parent ?: return null
				leftMatch
			}

		while(true)
		{
			val rightMatchI = leftMatch.rights.binarySearch(RightLookupEntry(right, ""))
			val rightMatch = if(rightMatchI >= 0)
					leftMatch.rights[rightMatchI]
				else
				{
					var rightMatch = leftMatch.rights.getOrNull((-rightMatchI - 1) - 1)
					while(rightMatch != null && !right.startsWith(rightMatch.right))
						rightMatch = rightMatch.parent
					rightMatch
				}

			if(rightMatch != null)
				return Replacement(leftMatch.left, rightMatch.right, rightMatch.replacement)

			leftMatch = leftMatch.parent ?: return null
		}
	}

	internal fun remove(leftI: Int, rightI: Int)
	{
		val leftEntry = this.replacements[leftI]
		val rightEntry = leftEntry.rights.removeAt(rightI)
		leftEntry.rights
			.subList(rightI, leftEntry.rights.size)
			.forEach({
				if(!it.right.startsWith(rightEntry.right))
					return@forEach

				if(it.parent == rightEntry)
					it.parent = rightEntry.parent
			})

		if(leftEntry.rights.isEmpty())
		{
			this.replacements.removeAt(leftI)

			this.replacements
				.subList(leftI, this.replacements.size)
				.forEach({
					if(!it.left.endsWith(leftEntry.left))
						return@forEach

					if(it.parent == leftEntry)
						it.parent = leftEntry.parent
				})
		}
	}

	fun remove(left: String, right: String)
	{
		@Suppress("NAME_SHADOWING")
		val left = left.toLowerCase(Locale.getDefault())
		@Suppress("NAME_SHADOWING")
		val right = right.toLowerCase(Locale.getDefault())

		val leftI = this.replacements.binarySearch(LeftLookupEntry(left))
		if(leftI < 0)
			return
		val leftEntry = this.replacements[leftI]

		val rightI = leftEntry.rights.binarySearch(RightLookupEntry(right, ""))
		if(rightI < 0)
			return

		this.remove(leftI, rightI)
	}

	fun add(left: String, right: String, replacement: String)
	{
		@Suppress("NAME_SHADOWING")
		val left = left.toLowerCase(Locale.getDefault())
		@Suppress("NAME_SHADOWING")
		val right = right.toLowerCase(Locale.getDefault())

		val leftEntryI = this.replacements.binarySearch(LeftLookupEntry(left))
		val leftEntry = if(leftEntryI < 0)
			{
				val leftInsertI = -leftEntryI - 1

				var leftParent: LeftLookupEntry? = null
				if(left.isNotEmpty())
				{
					// Parents must be shorter, not equal
					val left2 = left.substring(1)
					leftParent = this.replacements.getOrNull(leftInsertI - 1)
					while(leftParent != null && !left2.endsWith(leftParent.left))
						leftParent = leftParent.parent
				}

				val leftEntry = LeftLookupEntry(left, leftParent)
				this.replacements.add(leftInsertI, leftEntry)

				for(v in this.replacements.subList(leftInsertI + 1, this.replacements.size))
				{
					if(!v.left.endsWith(left))
						break

					if(v.parent?.compareTo(leftEntry) ?: -1 < 0)
						v.parent = leftEntry
				}

				leftEntry
			}
			else
				this.replacements[leftEntryI]

		val rightEntryI = leftEntry.rights.binarySearch(RightLookupEntry(right, ""))
		if(rightEntryI < 0)
		{
			val rightInsertI = -rightEntryI - 1

			var rightParent: RightLookupEntry? = null
			if(right.isNotEmpty())
			{
				// Parents must be shorter, not equal
				val right2 = right.substring(0, right.length - 1)
				rightParent = leftEntry.rights.getOrNull(rightInsertI - 1)
				while(rightParent != null && !right2.startsWith(rightParent.right))
					rightParent = rightParent.parent
			}

			val rightEntry = RightLookupEntry(right, replacement, rightParent)
			leftEntry.rights.add(rightInsertI, rightEntry)

			for(v in leftEntry.rights.subList(rightInsertI + 1, leftEntry.rights.size))
			{
				if(!v.right.startsWith(right))
					break

				if(v.parent?.compareTo(rightEntry) ?: -1 < 0)
					v.parent = rightEntry
			}
		}
		else
		{
			leftEntry.rights[rightEntryI] = RightLookupEntry(
				right,
				replacement,
				leftEntry.rights[rightEntryI].parent)
		}
	}

	override fun match(a: String, b: String): Orthography.Result? =
		this.find(a.toLowerCase(Locale.getDefault()), b.toLowerCase(Locale.getDefault()))?.let({
			Orthography.Result(
				it.left.length,
				it.replacement.withCase((a + b).substring(a.length - it.left.length))
					+ b.substring(it.right.length, b.length))
		})

	fun toJson(): JsonArray =
		this.replacements.map({ left ->
				left.rights.map({ right ->
					JsonObject().also({
						it.set("l", left.left)
						it.set("r", right.right)
						it.set("s", right.replacement)
					})
				})
			})
			.flatten()
			.toJson()

	companion object
	{
		fun fromJson(input: InputStream): SimpleOrthography =
			try
			{
				val json = input.bufferedReader()
					.use({ Json.parse(it) })
					.asArray()

				SimpleOrthography().also({ orthography ->
					json.forEach({
						val values = it.asObject()
						orthography.add(
							values.get("l").asString(),
							values.get("r").asString(),
							values.get("s").asString()
						)
					})
				})
			}
			catch(e: com.eclipsesource.json.ParseException)
			{
				throw FileParseException("Invalid JSON", e)
			}
			catch(e: java.lang.NullPointerException)
			{
				throw FileParseException("Missing value", e)
			}
			catch(e: java.lang.UnsupportedOperationException)
			{
				throw FileParseException("Invalid type", e)
			}
	}
}

fun SimpleOrthography.removeRedundant()
{
	var leftI = 0
	leftLoop@while(leftI < this.replacements.size)
	{
		val leftEntry = this.replacements[leftI]

		var rightI = 0
		rightLoop@while(rightI < leftEntry.rights.size)
		{
			val rightEntry = leftEntry.rights[rightI]
			val replacement = SimpleOrthography.Replacement(
				leftEntry.left,
				rightEntry.right,
				rightEntry.replacement)

			this.remove(leftI, rightI)

			if(replacement.replacement != this.apply(replacement.left, replacement.right))
				this.add(replacement.left, replacement.right, replacement.replacement)
			else
			{
				if(leftEntry.rights.isEmpty())
					continue@leftLoop
				continue@rightLoop
			}

			++rightI
		}

		++leftI
	}
}
