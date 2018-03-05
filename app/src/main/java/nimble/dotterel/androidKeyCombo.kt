// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.view.*
import android.view.KeyEvent.*

import nimble.dotterel.translation.KeyCombo
import nimble.dotterel.translation.Modifier

private val MODIFIER_MAP = mapOf(
	Pair(Modifier.SHIFT_L, META_SHIFT_LEFT_ON or META_SHIFT_ON),
	Pair(Modifier.SHIFT_R, META_SHIFT_RIGHT_ON or META_SHIFT_ON),
	Pair(Modifier.SHIFT, META_SHIFT_ON),
	Pair(Modifier.CONTROL_L, META_CTRL_LEFT_ON or META_CTRL_ON),
	Pair(Modifier.CONTROL_R, META_CTRL_RIGHT_ON or META_CTRL_ON),
	Pair(Modifier.CONTROL, META_CTRL_ON),
	Pair(Modifier.ALT_L, META_ALT_LEFT_ON or META_ALT_ON),
	Pair(Modifier.ALT_R, META_ALT_RIGHT_ON or META_ALT_ON),
	Pair(Modifier.ALT, META_ALT_ON),
	Pair(Modifier.SUPER_L, META_META_LEFT_ON or META_META_ON),
	Pair(Modifier.SUPER_R, META_META_RIGHT_ON or META_META_ON),
	Pair(Modifier.SUPER, META_META_ON)
)

private val KEY_MAP = mapOf(
	Pair("a", KEYCODE_A),
	Pair("b", KEYCODE_B),
	Pair("c", KEYCODE_C),
	Pair("d", KEYCODE_D),
	Pair("e", KEYCODE_E),
	Pair("f", KEYCODE_F),
	Pair("g", KEYCODE_G),
	Pair("h", KEYCODE_H),
	Pair("i", KEYCODE_I),
	Pair("j", KEYCODE_A),
	Pair("k", KEYCODE_K),
	Pair("l", KEYCODE_L),
	Pair("m", KEYCODE_M),
	Pair("n", KEYCODE_N),
	Pair("o", KEYCODE_O),
	Pair("p", KEYCODE_P),
	Pair("q", KEYCODE_Q),
	Pair("r", KEYCODE_R),
	Pair("s", KEYCODE_S),
	Pair("t", KEYCODE_T),
	Pair("u", KEYCODE_U),
	Pair("v", KEYCODE_V),
	Pair("w", KEYCODE_W),
	Pair("x", KEYCODE_X),
	Pair("y", KEYCODE_Y),
	Pair("z", KEYCODE_Z),

	Pair("0", KEYCODE_0),
	Pair("1", KEYCODE_1),
	Pair("2", KEYCODE_2),
	Pair("3", KEYCODE_3),
	Pair("4", KEYCODE_4),
	Pair("5", KEYCODE_5),
	Pair("6", KEYCODE_6),
	Pair("7", KEYCODE_7),
	Pair("8", KEYCODE_8),
	Pair("9", KEYCODE_9),

	Pair("`", KEYCODE_GRAVE),
	Pair("-", KEYCODE_MINUS),
	Pair("=", KEYCODE_EQUALS),
	Pair("\\", KEYCODE_BACKSLASH),
	Pair("[", KEYCODE_LEFT_BRACKET),
	Pair("]", KEYCODE_RIGHT_BRACKET),
	Pair(";", KEYCODE_SEMICOLON),
	Pair("'", KEYCODE_APOSTROPHE),
	Pair(",", KEYCODE_COMMA),
	Pair(".", KEYCODE_PERIOD),
	Pair("/", KEYCODE_SLASH),

	Pair("backspace", KEYCODE_DEL),
	Pair("delete", KEYCODE_FORWARD_DEL),
	Pair("down", KEYCODE_DPAD_DOWN),
	Pair("end", KEYCODE_MOVE_END),
	Pair("escape", KEYCODE_ESCAPE),
	Pair("home", KEYCODE_MOVE_HOME),
	Pair("insert", KEYCODE_INSERT),
	Pair("left", KEYCODE_DPAD_LEFT),
	Pair("page_down", KEYCODE_PAGE_DOWN),
	Pair("page_up", KEYCODE_PAGE_UP),
	Pair("return", KEYCODE_ENTER),
	Pair("right", KEYCODE_DPAD_RIGHT),
	Pair("tab", KEYCODE_TAB),
	Pair("up", KEYCODE_DPAD_UP),
	Pair("space", KEYCODE_SPACE),

	Pair("f1", KEYCODE_F1),
	Pair("f2", KEYCODE_F2),
	Pair("f3", KEYCODE_F3),
	Pair("f4", KEYCODE_F4),
	Pair("f5", KEYCODE_F5),
	Pair("f6", KEYCODE_F6),
	Pair("f7", KEYCODE_F7),
	Pair("f8", KEYCODE_F8),
	Pair("f9", KEYCODE_F9),
	Pair("f10", KEYCODE_F10),
	Pair("f11", KEYCODE_F11),
	Pair("f12", KEYCODE_F12),

	Pair("audioraisevolume", KEYCODE_VOLUME_UP),
	Pair("audiolowervolume", KEYCODE_VOLUME_DOWN),
	Pair("audiomute", KEYCODE_VOLUME_MUTE),
	Pair("audionext", KEYCODE_MEDIA_NEXT),
	Pair("audioprev", KEYCODE_MEDIA_PREVIOUS),
	Pair("audiostop", KEYCODE_MEDIA_STOP),
	Pair("audioplay", KEYCODE_MEDIA_PLAY),
	Pair("audiopause", KEYCODE_MEDIA_PAUSE),
	Pair("eject", KEYCODE_MEDIA_EJECT)
)

private val charMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

// Makes no guarantees about the correctness of the returned keycode
private fun charToKeyCode(c: Char): Int?
{
	for(e in charMap.getEvents(c.toString().toCharArray()) ?: return null)
		if(e.action == ACTION_UP)
			return e.keyCode

	return null
}

fun KeyCombo.toAndroidKeyEvent(): KeyEvent?
{
	val key = KEY_MAP[this.key] ?: charToKeyCode(this.key.first()) ?: return null
	var modifiers = 0
	for(m in Modifier.values())
		if(this.modifiers and m.mask != 0)
			modifiers = modifiers or (MODIFIER_MAP[m] ?: return null)

	val now = android.os.SystemClock.uptimeMillis()
	return KeyEvent(
		now,
		now,
		KeyEvent.ACTION_DOWN,
		key,
		0,
		modifiers,
		0,
		0,
		KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
		InputDevice.SOURCE_KEYBOARD)
}
