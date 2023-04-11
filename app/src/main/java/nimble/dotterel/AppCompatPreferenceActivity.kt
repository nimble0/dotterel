package nimble.dotterel

import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar

/**
 * A [android.preference.PreferenceActivity] which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
abstract class AppCompatPreferenceActivity : PreferenceActivity()
{

	@Deprecated("Deprecated in Java")
	override fun onCreate(savedInstanceState: Bundle?)
	{
		delegate.installViewFactory()
		delegate.onCreate(savedInstanceState)
		super.onCreate(savedInstanceState)
	}

	override fun onPostCreate(savedInstanceState: Bundle?)
	{
		super.onPostCreate(savedInstanceState)
		delegate.onPostCreate(savedInstanceState)
	}

	val supportActionBar: ActionBar?
		get() = delegate.supportActionBar

	fun setSupportActionBar(toolbar: Toolbar?)
	{
		delegate.setSupportActionBar(toolbar)
	}

	override fun getMenuInflater(): MenuInflater
	{
		return delegate.menuInflater
	}

	override fun setContentView(@LayoutRes layoutResID: Int)
	{
		delegate.setContentView(layoutResID)
	}

	override fun setContentView(view: View)
	{
		delegate.setContentView(view)
	}

	override fun setContentView(view: View, params: ViewGroup.LayoutParams)
	{
		delegate.setContentView(view, params)
	}

	override fun addContentView(view: View, params: ViewGroup.LayoutParams)
	{
		delegate.addContentView(view, params)
	}

	override fun onPostResume()
	{
		super.onPostResume()
		delegate.onPostResume()
	}

	override fun onTitleChanged(title: CharSequence, color: Int)
	{
		super.onTitleChanged(title, color)
		delegate.setTitle(title)
	}

	override fun onConfigurationChanged(newConfig: Configuration)
	{
		super.onConfigurationChanged(newConfig)
		delegate.onConfigurationChanged(newConfig)
	}

	@Deprecated("Deprecated in Java")
	override fun onStop()
	{
		super.onStop()
		delegate.onStop()
	}

	@Deprecated("Deprecated in Java")
	override fun onDestroy()
	{
		super.onDestroy()
		delegate.onDestroy()
	}

	override fun invalidateOptionsMenu()
	{
		delegate.invalidateOptionsMenu()
	}

	private val delegate: AppCompatDelegate by lazy {
		AppCompatDelegate.create(this, null)
	}
}
