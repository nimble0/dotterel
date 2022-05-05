// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AlertDialog

open class EditTextFragment : PreferenceFragment(), FragmentExitListener
{
	private var text: String = ""
	private var readOnly: Boolean = false
	private var textView: TextView? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View
	{
		if(this.preference == null)
			this.preference = this.loadPreference()

		this.text = this.preference?.let({
			it.preferenceDataStore?.getString(it.key, null)
		}) ?: ""
		this.readOnly = this.arguments?.getBoolean("readOnly") ?: true

		this.textView = if(this.readOnly)
			TextView(this.activity).also({
				it.setTextIsSelectable(true)
			})
		else
			EditText(this.activity).also({
				it.isFocusableInTouchMode = true
				it.isClickable = true
				it.isScrollContainer = true
			})
		this.textView?.setHorizontallyScrolling(true)
		this.textView?.textSize = 20.0f
		this.textView?.typeface = Typeface.MONOSPACE
		this.textView?.text = text
		this.textView?.gravity = Gravity.START or Gravity.TOP

		return this.textView!!
	}

	override fun onExit(exit: () -> Unit): Boolean
	{
		val text = this.textView?.text?.toString()
		if(this.readOnly || text == this.text || text == null)
			return true

		AlertDialog.Builder(this.requireContext()).also({ alert ->
			alert.setTitle(R.string.pref_systems_close_file)
			alert.setMessage(R.string.pref_systems_save_changes)
				.setPositiveButton(R.string.yes, { _, _ ->
					this.preference?.also({
						it.preferenceDataStore?.putString(it.key, text)
					})
					exit()
				})
				.setNegativeButton(R.string.no, { _, _ ->
					exit()
				})
				.setNeutralButton(android.R.string.cancel, { _, _ -> })
		}).create().show()

		return false
	}
}
