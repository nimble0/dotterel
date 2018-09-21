// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

@file:Suppress("UNUSED_PARAMETER")

package nimble.dotterel.translation

import java.util.Collections

private val backspaceWord = KeyCombo("backspace", Modifier.CONTROL.mask)

fun List<Any>.filterTextActions() =
	this.filter({ it is UnformattedText || it is FormattedText })

fun undoStroke(translator: Translator, arg: String)
	: TranslationPart
{
	var count = arg.toIntOrNull() ?: 1

	while(count > 0)
	{
		val popped = translator.pop() ?: break
		if(!popped.undoable)
			++count

		--count
	}

	if(count > 0)
		translator.push(HistoryTranslation(
			listOf(),
			listOf(),
			Collections.nCopies(count, backspaceWord),
			translator.context))

	return TranslationPart()
}

fun transform(translator: Translator, arg: String) =
	TranslationPart(listOf(UnformattedText(
		formatting = Formatting(
			spaceStart = Formatting.Space.NONE,
			spaceEnd = null,
			orthographyEnd = null,
			transform = translator.processor.transforms[arg],
			transformState = Formatting.TransformState.MAIN
		))))

fun singleTransform(translator: Translator, arg: String) =
	TranslationPart(listOf(UnformattedText(
		formatting = Formatting(
			spaceStart = Formatting.Space.NONE,
			spaceEnd = null,
			orthographyEnd = null,
			singleTransform = translator.processor.transforms[arg],
			transformState = Formatting.TransformState.MAIN
		))))

fun resetTransform(translator: Translator, arg: String) =
	TranslationPart(listOf(UnformattedText(formatting = Formatting(
		spaceStart = Formatting.Space.NONE,
		spaceEnd = null,
		orthographyEnd = null,
		transform = translator.system.defaultFormatting.transform))))

fun setSpace(translator: Translator, arg: String) =
	TranslationPart(listOf(UnformattedText(formatting = Formatting(
		space = arg,
		spaceStart = Formatting.Space.NONE,
		spaceEnd = null,
		orthographyEnd = null))))

fun resetSpace(translator: Translator, arg: String) =
	TranslationPart(listOf(UnformattedText(formatting = Formatting(
		space = translator.system.defaultFormatting.space,
		spaceStart = Formatting.Space.NONE,
		spaceEnd = null,
		orthographyEnd = null))))


fun repeatLastStroke(translator: Translator, arg: String)
	: TranslationPart
{
	var lastTranslation = translator.history.lastOrNull() ?: return TranslationPart()
	val lastStroke = lastTranslation.strokes.lastOrNull() ?: return TranslationPart()

	translator.apply(lastStroke)

	return TranslationPart()
}

fun lastTranslation(translator: Translator, arg: String) =
	translator.history.lastOrNull()?.let({ TranslationPart(it.actions) })
		?: TranslationPart()

// A cluster is a sequence of translations with no spaces between
fun lastClusterSize(translator: Translator): Int
{
	val iter = translator.history.asReversed().iterator()
	if(!iter.hasNext())
		return 0

	var i = 0
	var a: HistoryTranslation? = iter.next()
	do
	{
		val b = a as HistoryTranslation
		a = if(iter.hasNext()) iter.next() else null
		++i
	}
	while(a != null && b.hasText && a.text.formatting.noSpace(b.text.formatting))

	return i
}

fun lastCluster(translator: Translator, arg: String) =
	TranslationPart(translator.history
		.subList(
			translator.history.size - lastClusterSize(translator),
			translator.history.size)
		.flatMap({ it.actions }))

fun moveLastCluster(translator: Translator, arg: String)
	: TranslationPart =
	translator.popFull(lastClusterSize(translator))
		.fold(
			TranslationPart(),
			{ acc, it ->
				TranslationPart(
					acc.actions + it.actions.filterTextActions(),
					acc.replaces + listOf(it))
			})

fun retroBreakTranslation(translator: Translator, arg: String)
	: TranslationPart
{
	var lastTranslation = translator.popFull()

	// Skip translations that can't be broken (because they consist of only one stroke)
	val skipTranslations = mutableListOf<HistoryTranslation>()
	while(lastTranslation != null && lastTranslation.strokes.size <= 1)
	{
		skipTranslations.add(0, lastTranslation)
		lastTranslation = translator.popFull()
	}

	// Couldn't find a translation to break so just restore original state
	if(lastTranslation == null)
	{
		for(h in skipTranslations)
			translator.push(h)
		return TranslationPart()
	}

	val lastStroke = lastTranslation.strokes.last()

	// tailAction = translation of the last stroke of the broken translation
	// headActions = translations that were replaced when the last stroke of the broken
	//              translation was added
	//
	// eg/ With translations:
	// "SWEUZ": "swiz"
	// "ER": "er"
	// "LAND": "land"
	// "SWEUZ/ER/LAND": "Switzerland"
	//
	// tailAction = "land"
	// headActions = "swiz er"
	val tailAction = translator.processor.process(
		translator,
		translator.dictionary[listOf(lastStroke)] ?: lastStroke.rtfcre)
	val headActions = TranslationPart(
		lastTranslation.replaces.flatMap({
			it.actions.filterTextActions() }),
		listOf(lastTranslation))

	// Translations skipped because they cannot be broken
	// (because they consist of only one stroke)
	val skippedAction = TranslationPart(
		skipTranslations.flatMap({
			it.actions.filterTextActions() }),
		skipTranslations)

	return headActions + tailAction + skippedAction
}

fun retroToggleAsterisk(translator: Translator, arg: String)
	: TranslationPart
{
	var lastTranslation = translator.pop() ?: return TranslationPart()
	val lastStroke = lastTranslation.strokes.last()

	val asteriskStroke = lastStroke.layout.parseKeys(listOf("*"))
	val toggledStroke = Stroke(
		lastStroke.layout,
		lastStroke.keys xor asteriskStroke.keys)

	translator.apply(toggledStroke)

	return TranslationPart()
}
