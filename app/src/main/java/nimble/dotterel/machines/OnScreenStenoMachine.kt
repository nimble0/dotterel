package nimble.dotterel.machines

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.constraintlayout.widget.ConstraintLayout

import nimble.dotterel.*
import nimble.dotterel.translation.*
import nimble.dotterel.util.*

val ON_SCREEN_MACHINE_STYLES = mapOf(
	Pair("Touch",
		mapOf(Pair("#STKPWHR-AO*EU-FRPBLGTSDZ", R.layout.touch_steno))),
	Pair("Swipe",
		mapOf(Pair("#STKPWHR-AO*EU-FRPBLGTSDZ", R.layout.swipe_steno)))
)

class OnScreenStenoMachine(private val app: Dotterel) :
	StenoMachine, StenoMachine.Listener, Dotterel.EditorListener
{
	override var keyLayout: KeyLayout = EMPTY_KEY_LAYOUT
		set(v)
		{
			field = v
			this.updateStyle()
		}
	override var strokeListener: StenoMachine.Listener? = app

	class Factory : StenoMachine.Factory
	{
		override fun makeStenoMachine(app: Dotterel) =
			OnScreenStenoMachine(app).also({ app.addEditorListener(it) })
	}

	private fun updateStyle()
	{
		val style = this.app.preferences?.getString("machine/On Screen/style", null)
		val viewId = ON_SCREEN_MACHINE_STYLES[style]?.get(this.keyLayout.pureKeysString)
		if(viewId == null)
		{
			val m = "On Screen machine style $style does not support the current key layout"
			Log.e("Dotterel", m)
			Toast.makeText(this.app, m, Toast.LENGTH_SHORT).show()
		}
		this.app.viewId = viewId
	}

	override fun preferenceChanged(preferences: SharedPreferences, key: String)
	{
		when(key)
		{
			"machine/On Screen/style" -> this.updateStyle()
		}
	}

	override fun changeStroke(s: Stroke) = this.strokeListener?.changeStroke(s) ?: Unit
	override fun applyStroke(s: Stroke) = this.strokeListener?.applyStroke(s) ?: Unit

	override fun close()
	{
		this.app.viewId = null
	}

	override fun setInputView(v: View?)
	{
		if(v is StenoView)
		{
			v.outputKeyLayout = this.keyLayout
			v.strokeListener = this
			v.translator = this.app.translator
		}
	}
}

abstract class StenoView(context: Context, attributes: AttributeSet) :
	ConstraintLayout(context, attributes)
{
	protected var translationPreview: TextView? = null
	protected var keys = listOf<TextView>()
	abstract val keyLayout: KeyLayout
	var outputKeyLayout: KeyLayout = KeyLayout("")
	var strokeListener: StenoMachine.Listener? = null
	var translator: Translator? = null

	open val stroke: Stroke
		get() = this.keyLayout.parseKeys(this.keys
			.filter({ it.isSelected })
			.map({ it.hint as String }))

	override fun onFinishInflate()
	{
		super.onFinishInflate()

		val keys = mutableListOf<TextView>()
		while(true)
		{
			val key = this.findViewWithTag<TextView>("steno_key") ?: break

			key.tag = null
			keys.add(key)
		}
		this.keys = keys

		this.translationPreview = this.findViewById(R.id.translation_preview)
	}

	@SuppressLint("SetTextI18n")
	protected open fun updatePreview(s: Stroke)
	{
		if(s.isEmpty)
			this.translationPreview?.text = ""
		else
		{
			val rtfcre = s.rtfcre
			val translation = this.translator?.translate(s)?.raw ?: ""
			this.translationPreview?.text = "$rtfcre : ${translation.trim()}"
		}
	}

	protected open fun keyAt(p: Vector2): TextView? = this.keys.find(
		{ (this.position + p) in Box(it.position, it.position + it.size) })

	protected open fun changeStroke()
	{
		val stroke = Stroke(this.outputKeyLayout, this.stroke.keys)
		this.strokeListener?.changeStroke(stroke)
		this.updatePreview(stroke)
	}

	protected open fun applyStroke()
	{
		if(this.stroke.isEmpty)
			return

		this.strokeListener?.applyStroke(Stroke(this.outputKeyLayout, this.stroke.keys))

		for(key in this.keys)
			key.isSelected = false
		this.updatePreview(Stroke(this.outputKeyLayout, this.stroke.keys))
	}
}
