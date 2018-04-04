// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.annotation.SuppressLint
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

class AboutFragment : PreferenceFragment()
{
	@SuppressLint("SetJavaScriptEnabled")
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup,
		savedInstanceState: Bundle?
	): View
	{
		val webView = WebView(inflater.context)
		webView.settings.javaScriptEnabled = true
		webView.loadUrl(this.getString(R.string.about_page_url)
			+ "?version=" + this.getString(R.string.version_name))

		return webView
	}
}
