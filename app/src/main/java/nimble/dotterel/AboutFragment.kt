// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.webkit.WebView

import androidx.core.content.ContextCompat

import androidx.fragment.app.Fragment

class AboutFragment : Fragment(R.layout.about)
{
	@SuppressLint("SetJavaScriptEnabled")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?)
	{
		super.onViewCreated(view, savedInstanceState)

		val webView = view as WebView
		val nightModeFlags = this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
		val darkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

		webView.settings.javaScriptEnabled = true
		webView.loadUrl(this.getString(R.string.pref_about_page_url)
			+ "?version=" + this.getString(R.string.version_name) + "&darkMode=$darkMode")
		webView.setBackgroundColor(ContextCompat.getColor(this.requireContext(), R.color.background))
	}
}
