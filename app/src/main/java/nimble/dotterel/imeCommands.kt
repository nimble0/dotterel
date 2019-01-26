// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

@file:Suppress("UNUSED_PARAMETER")

package nimble.dotterel

import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager

import nimble.dotterel.translation.TranslationPart
import nimble.dotterel.translation.Translator

fun editorAction(translator: Translator, arg: String) =
	TranslationPart(actions = listOf(
		DotterelRunnable.make({ dotterel: Dotterel ->
			dotterel.sendDefaultEditorAction(false)
		})
	))

fun switchPreviousIme(translator: Translator, arg: String)
	: TranslationPart
{
	return TranslationPart(
		actions = listOf(DotterelRunnable.make({ dotterel: Dotterel ->
			if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
				dotterel.switchToPreviousInputMethod()
			else
			{
				val imm = dotterel.getSystemService(Context.INPUT_METHOD_SERVICE)
					as InputMethodManager
				@Suppress("DEPRECATION")
				imm.switchToLastInputMethod(dotterel.window?.window?.attributes?.token)
			}
		})))
}

fun switchNextIme(translator: Translator, arg: String)
	: TranslationPart
{
	return TranslationPart(
		actions = listOf(DotterelRunnable.make({ dotterel: Dotterel ->
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
		})))
}

fun switchIme(translator: Translator, arg: String)
	: TranslationPart
{
	return TranslationPart(
		actions = listOf(DotterelRunnable.make({ dotterel: Dotterel ->
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
		})))
}

fun showImePicker(translator: Translator, arg: String)
	: TranslationPart
{
	return TranslationPart(
		actions = listOf(DotterelRunnable.make({ dotterel: Dotterel ->
			val imm = dotterel.getSystemService(Context.INPUT_METHOD_SERVICE)
				as InputMethodManager
			imm.showInputMethodPicker()
		})))
}

