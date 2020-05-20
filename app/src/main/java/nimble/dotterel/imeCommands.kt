// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

@file:Suppress("UNUSED_PARAMETER")

package nimble.dotterel

import android.content.Context
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

import nimble.dotterel.translation.Translator

fun editorAction(dotterel: Dotterel, translator: Translator, arg: String)
{
	if(dotterel.currentInputEditorInfo.actionLabel != null)
		dotterel.sendDefaultEditorAction(false)
	else
		dotterel.currentInputConnection.performEditorAction(
			EditorInfo.IME_ACTION_DONE)
}

fun switchPreviousIme(dotterel: Dotterel, translator: Translator, arg: String)
{
	if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
		dotterel.switchToPreviousInputMethod()
	else
	{
		val imm = dotterel.getSystemService(Context.INPUT_METHOD_SERVICE)
			as InputMethodManager
		@Suppress("DEPRECATION")
		imm.switchToLastInputMethod(dotterel.window?.window?.attributes?.token)
	}
}

fun switchNextIme(dotterel: Dotterel, translator: Translator, arg: String)
{
	if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
		dotterel.switchToNextInputMethod(false)
	else
	{
		val imm = dotterel.getSystemService(Context.INPUT_METHOD_SERVICE)
			as InputMethodManager
		@Suppress("DEPRECATION")
		imm.switchToNextInputMethod(
			dotterel.window?.window?.attributes?.token,
			false)
	}
}

fun switchIme(dotterel: Dotterel, translator: Translator, arg: String)
{
	try
	{
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
			dotterel.switchInputMethod(arg)
		else
		{
			val imm = dotterel.getSystemService(Context.INPUT_METHOD_SERVICE)
				as InputMethodManager
			@Suppress("DEPRECATION")
			imm.setInputMethod(
				dotterel.window?.window?.attributes?.token,
				arg)
		}
	}
	catch(e: java.lang.IllegalArgumentException)
	{
		Log.e("Dotterel IME:SWITCH", "Bad IME id")
	}
}

fun showImePicker(dotterel: Dotterel, translator: Translator, arg: String)
{
	val imm = dotterel.getSystemService(Context.INPUT_METHOD_SERVICE)
		as InputMethodManager
	imm.showInputMethodPicker()
}
