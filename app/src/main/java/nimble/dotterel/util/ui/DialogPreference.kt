// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet

import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat

abstract class DialogPreference : androidx.preference.DialogPreference
{
	constructor(
		context: Context,
		attrs: AttributeSet?,
		defStyleAttr: Int,
		defStyleRes: Int
	) :
		super(context, attrs, defStyleAttr, defStyleRes)

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
		super(context, attrs, defStyleAttr)

	constructor(context: Context, attrs: AttributeSet?) :
		super(context, attrs)

	constructor(context: Context) :
		super(context)

	abstract val dialogFragment: PreferenceDialogFragmentCompat
}

fun PreferenceFragmentCompat.displayPreferenceDialog(
	preference: DialogPreference,
	dialogFragmentTag: String?)
{
	val fragment = preference.dialogFragment
	fragment.arguments = Bundle(1)
		.also({ it.putString("key", preference.key) })
	fragment.setTargetFragment(this, 0)
	fragment.show(this.requireFragmentManager(), dialogFragmentTag)
}
