// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

abstract class PreferenceFragment : PreferenceFragmentCompat()
{
	open var preference: Preference? = null
}
