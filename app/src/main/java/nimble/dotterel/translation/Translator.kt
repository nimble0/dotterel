// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import java.text.ParseException

import kotlin.math.max

import nimble.dotterel.Dotterel
import nimble.dotterel.util.product

private const val TRANSLATION_HISTORY_SIZE = 100

fun format(
	context: FormattedText,
	actions: List<Any>): List<Any>
{
	@Suppress("NAME_SHADOWING")
	var context = context
	val output = mutableListOf<Any>()
	for(a in actions)
		if(a is UnformattedText)
		{
			val f = a.format(context)
			output.add(f)
			context += f
		}
		else
		{
			output.add(a)
			if(a is FormattedText)
				context += a
		}

	return output
}

fun actionsToText(actions: List<Any>): FormattedText? =
	@Suppress("NAME_SHADOWING")
	actions.filterIsInstance<FormattedText>().let({ actions ->
		if(actions.isEmpty())
			null
		else
			actions.reduce({ acc, it -> acc + it})
	})

fun joinTextActions(actions: List<Any>): List<Any>
{
	val actions2 = mutableListOf<Any>()
	for(a in actions)
	{
		val previousAction = actions2.lastOrNull()
		if(previousAction is FormattedText && a is FormattedText)
			actions2[actions2.lastIndex] = previousAction + a
		else
			actions2.add(a)
	}

	return actions2
}

fun removeEmptyTextActions(actions: List<Any>): List<Any> =
	actions.filterNot(
		{ it is FormattedText && it.backspaces == 0 && it.text.isEmpty() })

// Optimise text actions to not delete and output the same text
// eg/
// context = "work"
// action = FormattedText(backspaces = 4, text = "working")
// optimizedAction = FormattedText(backspaces = 0, text = "ing")
fun optimiseTextAction(context: String, action: FormattedText): FormattedText =
	if(action.backspaces <= context.length)
	{
		val deleted = context.substring(context.length - action.backspaces)
		val common = deleted.commonPrefixWith(action.text)

		FormattedText(
			action.backspaces - common.length,
			action.text.substring(common.length),
			action.formatting)
	}
	else
		action

fun optimiseTextActions(context: String, actions: List<Any>): List<Any>
{
	@Suppress("NAME_SHADOWING")
	var context = context

	return actions.map({
		if(it is FormattedText)
			optimiseTextAction(context, it).also({
				context = context.substring(
					0,
					max(0, context.length - it.backspaces)
				) + it.text
			})
		else
			it
	})
}

fun optimiseActions(context: String, actions: List<Any>): List<Any>
{
	val actions2 = joinTextActions(actions)
	val actions3 = optimiseTextActions(context, actions2)
	return removeEmptyTextActions(actions3)
}

class Translator(
	var system: System,
	var dotterel: Dotterel? = null,
	var log: Log = object : Log {})
{
	val processor = TranslationProcessor(this)

	private val _history = mutableListOf<HistoryTranslation>()

	private var preBufferedContext = ""
	private var bufferedActions = mutableListOf<Any>()

	val context: FormattedText
		get()
		{
			val contextStr = StringBuilder()
			for(h in this._history)
			{
				contextStr.setLength(
					Math.max(0, contextStr.length - h.replacesText.length))
				contextStr.append(h.text.text)
			}

			return FormattedText(
				0,
				contextStr.toString(),
				this._history.lastOrNull()?.text?.formatting
					?: this.system.defaultFormatting
			)
		}

	val history: List<HistoryTranslation>
		get() = this._history

	private fun lookup(
		strokes: List<Stroke>,
		replaces: List<HistoryTranslation>,
		prefixes: List<Stroke>,
		suffixes: List<Stroke>
	): Translation?
	{
		val tryStrokes = strokes.toMutableList()
		for(a in product(listOf(prefixes.size, suffixes.size)))
		{
			val prefixStrokes = prefixes[a[0]]
			val suffixStrokes = suffixes[a[1]]

			tryStrokes[0] = strokes[0]
			tryStrokes[tryStrokes.lastIndex] = strokes.last() - suffixStrokes
			tryStrokes[0] = tryStrokes[0] - prefixStrokes

			val main = this.system.dictionaries[tryStrokes]
			if(main != null)
			{
				val prefix = this.system.dictionaries[listOf(prefixStrokes)] ?: ""
				val suffix = this.system.dictionaries[listOf(suffixStrokes)] ?: ""

				return Translation(
					strokes,
					replaces,
					"$prefix $main $suffix".trim(),
					false,
					!prefixStrokes.isEmpty,
					!suffixStrokes.isEmpty)
			}
		}

		return null
	}

	fun translate(s: Stroke): Translation
	{
		if(s.layout != this.system.keyLayout)
			throw IllegalArgumentException("s.layout must match system.keyLayout")

		val emptyAffix = listOf(Stroke(this.system.keyLayout, 0L))
		val prefixStrokes = emptyAffix + this.system.prefixStrokes
		val suffixStrokes = emptyAffix + this.system.suffixStrokes

		var translation = this.lookup(
				listOf(s),
				listOf(),
				prefixStrokes,
				suffixStrokes)
			?: Translation(listOf(s), listOf(), s.rtfcre, true, false, false)

		val strokes = mutableListOf(s)
		val replaces = mutableListOf<HistoryTranslation>()
		for(h in this._history.reversed())
		{
			strokes.addAll(0, h.strokes)
			replaces.add(0, h)

			if(strokes.size > this.system.dictionaries.longestKey)
				break

			val allowPrefixes = replaces.first().allowPrefixedReplacement
			val allowSuffixes = translation.hasSuffix || translation.isUntranslate

			translation = this.lookup(
					strokes.toList(),
					replaces.toList(),
					if(allowPrefixes) prefixStrokes else emptyAffix,
					if(allowSuffixes) suffixStrokes else emptyAffix)
				?: translation
		}

		return translation
	}

	private fun limitHistorySize()
	{
		val drop = this._history.size - TRANSLATION_HISTORY_SIZE
		if(drop > 0)
			this._history.subList(0, drop).clear()
	}

	fun push(t: HistoryTranslation)
	{
		this._history.add(t)
		this.bufferedActions.addAll(t.formattedActions)
		this.limitHistorySize()
	}

	fun push(t: Translation)
	{
		try
		{
			for(h in t.replaces.asReversed())
				if(h != this._history.last())
					throw IllegalArgumentException(
						"Replaced translations must match translation history buffer")
				else
					this.popFull()

			val processed = this.processor.process(t.raw)
			if(!processed.empty)
				this.push(HistoryTranslation(
					t.strokes,
					processed.replaces + t.replaces,
					processed.actions,
					this.context,
					t.hasPrefix || t.isUntranslate))
		}
		catch(e: ParseException)
		{
			this.log.error("Error parsing translation: ${t.raw}")
		}
	}

	// Pop and restore replaced translations
	fun pop(): HistoryTranslation?
	{
		val removed = this.popFull() ?: return null
		for(h in removed.replaces)
			this._history.add(h)
		this.limitHistorySize()
		this.bufferedActions.add(removed.redoReplacedAction)
		return removed
	}

	// Pop and don't restore replaced translations
	fun popFull(): HistoryTranslation?
	{
		if(this._history.isEmpty())
			return null

		val removed = this._history.removeAt(this._history.lastIndex)
		this.bufferedActions.add(removed.undoAction)
		return removed
	}

	fun apply(s: Stroke) = this.push(this.translate(s))

	fun flush(): List<Any>
	{
		val actions = optimiseActions(this.preBufferedContext, this.bufferedActions)
		this.bufferedActions = mutableListOf()
		this.preBufferedContext = this.context.text
		return actions
	}
}

fun Translator.popFull(count: Int): List<HistoryTranslation>
{
	@Suppress("NAME_SHADOWING")
	var count = count
	return generateSequence({ if(--count >= 0) this.popFull() else null })
		.toList()
		.asReversed()
}

fun Translator.pop(count: Int): List<HistoryTranslation>
{
	@Suppress("NAME_SHADOWING")
	var count = count
	return generateSequence({ if(--count >= 0) this.pop() else null })
		.toList()
		.asReversed()
}
