// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import java.text.ParseException
import java.util.Locale

enum class Modifier(val mask: Int)
{
	SHIFT_L(1 shl 0),
	SHIFT_R(1 shl 1),
	SHIFT(1 shl 2),
	CONTROL_L(1 shl 3),
	CONTROL_R(1 shl 4),
	CONTROL(1 shl 5),
	ALT_L(1 shl 6),
	ALT_R(1 shl 7),
	ALT(1 shl 8),
	SUPER_L(1 shl 9),
	SUPER_R(1 shl 10),
	SUPER(1 shl 11),

	OPTION(Modifier.ALT.mask),
	WINDOWS(Modifier.SUPER.mask),
	COMMAND(Modifier.SUPER.mask),
}

private val KEYNAME_TO_CHAR = mapOf(
	Pair("aacute"            , '\u00e1'), // á
	Pair("acircumflex"       , '\u00e2'), // â
	Pair("acute"             , '\u00b4'), // ´
	Pair("adiaeresis"        , '\u00e4'), // ä
	Pair("ae"                , '\u00e6'), // æ
	Pair("agrave"            , '\u00e0'), // à
	Pair("ampersand"         ,      '&'), // &
	Pair("apostrophe"        ,     '\''), // '
	Pair("aring"             , '\u00e5'), // å
	Pair("asciicircum"       ,      '^'), // ^
	Pair("asciitilde"        ,      '~'), // ~
	Pair("asterisk"          ,      '*'), // *
	Pair("at"                ,      '@'), // @
	Pair("atilde"            , '\u00e3'), // ã
	Pair("backslash"         ,     '\\'), // \
	Pair("bar"               ,      '|'), // |
	Pair("braceleft"         ,      '{'), // {
	Pair("braceright"        ,      '}'), // }
	Pair("bracketleft"       ,      '['), // [
	Pair("bracketright"      ,      ']'), // ]
	Pair("brokenbar"         , '\u00a6'), // ¦
	Pair("ccedilla"          , '\u00e7'), // ç
	Pair("cedilla"           , '\u00b8'), // ¸
	Pair("cent"              , '\u00a2'), // ¢
	Pair("clear"             , '\u000b'), // 
	Pair("colon"             ,      ':'), // :
	Pair("comma"             ,      ','), // ,
	Pair("copyright"         , '\u00a9'), // ©
	Pair("currency"          , '\u00a4'), // ¤
	Pair("degree"            , '\u00b0'), // °
	Pair("diaeresis"         , '\u00a8'), // ¨
	Pair("division"          , '\u00f7'), // ÷
	Pair("dollar"            ,      '$'), // $
	Pair("eacute"            , '\u00e9'), // é
	Pair("ecircumflex"       , '\u00ea'), // ê
	Pair("ediaeresis"        , '\u00eb'), // ë
	Pair("egrave"            , '\u00e8'), // è
	Pair("equal"             ,      '='), // =
	Pair("eth"               , '\u00f0'), // ð
	Pair("exclam"            ,      '!'), // !
	Pair("exclamdown"        , '\u00a1'), // ¡
	Pair("grave"             ,      '`'), // `
	Pair("greater"           ,      '>'), // >
	Pair("guillemotleft"     , '\u00ab'), // «
	Pair("guillemotright"    , '\u00bb'), // »
	Pair("hyphen"            , '\u00ad'), // ­
	Pair("iacute"            , '\u00ed'), // í
	Pair("icircumflex"       , '\u00ee'), // î
	Pair("idiaeresis"        , '\u00ef'), // ï
	Pair("igrave"            , '\u00ec'), // ì
	Pair("less"              ,      '<'), // <
	Pair("macron"            , '\u00af'), // ¯
	Pair("masculine"         , '\u00ba'), // º
	Pair("minus"             ,      '-'), // -
	Pair("mu"                , '\u00b5'), // µ
	Pair("multiply"          , '\u00d7'), // ×
	Pair("nobreakspace"      , '\u00a0'), //
	Pair("notsign"           , '\u00ac'), // ¬
	Pair("ntilde"            , '\u00f1'), // ñ
	Pair("numbersign"        ,      '#'), // #
	Pair("oacute"            , '\u00f3'), // ó
	Pair("ocircumflex"       , '\u00f4'), // ô
	Pair("odiaeresis"        , '\u00f6'), // ö
	Pair("ograve"            , '\u00f2'), // ò
	Pair("onehalf"           , '\u00bd'), // ½
	Pair("onequarter"        , '\u00bc'), // ¼
	Pair("onesuperior"       , '\u00b9'), // ¹
	Pair("ooblique"          , '\u00d8'), // Ø
	Pair("ordfeminine"       , '\u00aa'), // ª
	Pair("oslash"            , '\u00f8'), // ø
	Pair("otilde"            , '\u00f5'), // õ
	Pair("paragraph"         , '\u00b6'), // ¶
	Pair("parenleft"         ,      '('), // (
	Pair("parenright"        ,      ')'), // )
	Pair("percent"           ,      '%'), // %
	Pair("period"            ,      '.'), // .
	Pair("periodcentered"    , '\u00b7'), // ·
	Pair("plus"              ,      '+'), // +
	Pair("plusminus"         , '\u00b1'), // ±
	Pair("question"          ,      '?'), // ?
	Pair("questiondown"      , '\u00bf'), // ¿
	Pair("quotedbl"          ,      '"'), // "
	Pair("quoteleft"         ,      '`'), // `
	Pair("quoteright"        ,     '\''), // '
	Pair("registered"        , '\u00ae'), // ®
	Pair("section"           , '\u00a7'), // §
	Pair("semicolon"         ,      ';'), // ;
	Pair("slash"             ,      '/'), // /
	Pair("space"             ,      ' '), //
	Pair("ssharp"            , '\u00df'), // ß
	Pair("sterling"          , '\u00a3'), // £
	Pair("tab"               ,     '\t'), //
	Pair("thorn"             , '\u00fe'), // þ
	Pair("threequarters"     , '\u00be'), // ¾
	Pair("threesuperior"     , '\u00b3'), // ³
	Pair("twosuperior"       , '\u00b2'), // ²
	Pair("uacute"            , '\u00fa'), // ú
	Pair("ucircumflex"       , '\u00fb'), // û
	Pair("udiaeresis"        , '\u00fc'), // ü
	Pair("ugrave"            , '\u00f9'), // ù
	Pair("underscore"        ,      '_'), // _
	Pair("yacute"            , '\u00fd'), // ý
	Pair("ydiaeresis"        , '\u00ff'), // ÿ
	Pair("yen"               , '\u00a5')  // ¥
)

data class KeyCombo(
	val key: String,
	val modifiers: Int)

private val KEY_COMBOS_PATTERN = Regex("\\A(?:(?:(\\w+)\\s*\\()|(\\))|(\\w+)|\\s+)")

fun parseKeyCombos(keyCombos: String): List<KeyCombo>
{
	var modifiers = 0
	val modifiersStack = mutableListOf<Modifier>()
	val actions = mutableListOf<KeyCombo>()

	var i = 0
	while(true)
	{
		val match = KEY_COMBOS_PATTERN.find(keyCombos.substring(i)) ?: break
		i += match.range.endInclusive + 1

		when
		{
			// Push modifier
			match.groupValues[1].isNotEmpty() ->
			{
				val modifier = try
				{
					Modifier.valueOf(match.groupValues[1].uppercase())
				}
				catch(e: IllegalArgumentException)
				{
					throw ParseException("Invalid modifier", i)
				}

				if(modifiers and modifier.mask > 0)
					throw ParseException("Modifier already set", i)
				modifiersStack.add(modifier)
				modifiers = modifiers or modifier.mask
			}
			// Pop modifier
			match.groupValues[2].isNotEmpty() ->
			{
				if(modifiersStack.isEmpty())
					throw ParseException("Missing open parentheses", i)
				val modifier = modifiersStack.removeAt(modifiersStack.size - 1)
				modifiers = modifiers and modifier.mask.inv()
			}
			// Key press
			match.groupValues[3].isNotEmpty() ->
			{
				val keyName = match.groupValues[3].lowercase()
				val key = KEYNAME_TO_CHAR[keyName]?.toString() ?: keyName
				actions.add(KeyCombo(key, modifiers))
			}
		}
	}

	if(!modifiersStack.isEmpty())
		throw ParseException("Missing close parentheses", i)

	if(i != keyCombos.length)
		throw ParseException("Error parsing key combos", i)

	return actions
}
