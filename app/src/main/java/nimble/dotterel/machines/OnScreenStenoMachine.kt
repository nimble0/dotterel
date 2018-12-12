package nimble.dotterel.machines

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.constraintlayout.widget.ConstraintLayout

import com.eclipsesource.json.JsonObject

import nimble.dotterel.*
import nimble.dotterel.translation.*
import nimble.dotterel.util.*

val ON_SCREEN_MACHINE_STYLES = mapOf(
	Pair("Touch", mapOf(
		Pair("Ireland", R.layout.touch_steno)
	)),
	Pair("Swipe", mapOf(
		Pair("Ireland", R.layout.swipe_steno)
	))
)

class OnScreenStenoMachine(private val app: Dotterel) :
	StenoMachine, StenoMachine.Listener, Dotterel.EditorListener
{
	private var keyLayout: KeyLayout = KeyLayout("")
	private var keyMap: Map<String, String> = mapOf()
	override var strokeListener: StenoMachine.Listener? = app

	class Factory : StenoMachine.Factory
	{
		override fun makeStenoMachine(app: Dotterel) =
			OnScreenStenoMachine(app).also({ app.addEditorListener(it) })
	}

	override fun setConfig(
		keyLayout: KeyLayout,
		config: JsonObject,
		systemConfig: JsonObject)
	{
		try
		{
			this.keyLayout = keyLayout

			val style = config.getString("style", null)!!
			val styleConfig = systemConfig.get(listOf("styles", style))?.asObject()
				?: JsonObject()
			val viewLayout = styleConfig.getString("viewLayout", null)

			this.keyMap = styleConfig
				.get("layout").asObject()
				.mapValues({ it.value.asArray().map({ key -> key.asString() }) })
				.let({
					val invertedMap = mutableMapOf<String, String>()
					for(mapping in it)
						for(key in mapping.value)
							invertedMap[key] = mapping.key
					invertedMap
				})

			val viewId = ON_SCREEN_MACHINE_STYLES[style]?.get(viewLayout)

			if(viewId == null)
			{
				val m = "Current system does not support On Screen machine style $style"
				Log.w("Steno Machine", m)
				Toast.makeText(this.app, m, Toast.LENGTH_SHORT).show()
			}
			this.app.viewId = viewId
		}
		catch(e: java.lang.NullPointerException)
		{
			throw IllegalArgumentException(e)
		}
		catch(e: java.lang.UnsupportedOperationException)
		{
			throw IllegalArgumentException(e)
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
			v.keyLayout = this.keyLayout
			v.keyMap = this.keyMap
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
	var keyLayout: KeyLayout = KeyLayout("")
	var strokeListener: StenoMachine.Listener? = null
	var translator: Translator? = null

	var keyMap: Map<String, String> = mapOf()
		set(v)
		{
			field = v
			for(key in this.keys)
				if(key.hint in keyMap)
				{
					key.visibility = View.VISIBLE
					key.text = keyMap[key.hint]?.trim('-')
				}
				else
					key.visibility = View.INVISIBLE
		}
	open val stroke: Stroke
		get() = this.keyLayout.parseKeys(this.keys
			.filter({ it.isSelected })
			.mapNotNull({ this.keyMap[it.hint] }))

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
		this.strokeListener?.changeStroke(this.stroke)
		this.updatePreview(this.stroke)
	}

	protected open fun applyStroke()
	{
		if(this.stroke.isEmpty)
			return

		this.strokeListener?.applyStroke(this.stroke)

		for(key in this.keys)
			key.isSelected = false
		this.updatePreview(this.stroke)
	}
}
