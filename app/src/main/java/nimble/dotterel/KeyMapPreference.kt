// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.*

import androidx.preference.*

import com.eclipsesource.json.*

import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.translation.systems.IRELAND_LAYOUT
import nimble.dotterel.util.*
import nimble.dotterel.util.DialogPreference

data class KeyMapping(val stenoKey: String, val keyboardKeys: MutableList<String>)

fun List<KeyMapping>.toJson(): JsonObject
{
	val json = JsonObject()
	for(mapping in this)
		json.add(mapping.stenoKey, mapping.keyboardKeys.toJson())
	return json
}

fun keyMapFromJson(key: String, json: String): Map<String, List<String>>
{
	try
	{
		val keyMap = mutableMapOf<String, List<String>>()
		for(mapping in Json.parse(json).asObject())
			keyMap[mapping.name] = mapping.value.asArray().map({ it.asString() })
		return keyMap
	}
	catch(e: com.eclipsesource.json.ParseException)
	{
		Log.e("Dotterel", "Preference $key has badly formed JSON")
	}
	catch(e: java.lang.UnsupportedOperationException)
	{
		Log.e("Dotterel", "Invalid type found while reading preference $key")
	}

	return mapOf()
}

fun keyCodeToString(keyCode: Int) =
	KeyEvent.keyCodeToString(keyCode).substring("KEYCODE_".length).toLowerCase()

fun stringToKeyCode(keyCodeString: String) =
	KeyEvent.keyCodeFromString("KEYCODE_${keyCodeString.toUpperCase()}")

class KeyMapAdapter(context: Context, items: MutableList<KeyMapping>) :
	ArrayAdapter<KeyMapping>(context, R.layout.pref_key_mapping, items)
{
	private fun addMapping(i: Int, keyCode: String)
	{
		val item = this.getItem(i) ?: return
		if(keyCode !in item.keyboardKeys)
			item.keyboardKeys.add(keyCode)
		this.notifyDataSetChanged()
	}
	private fun removeMapping(i: Int, keyCode: String)
	{
		val item = this.getItem(i) ?: return
		item.keyboardKeys.remove(keyCode)
		this.notifyDataSetChanged()
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup)
		: View
	{
		val inflater = LayoutInflater.from(this.context)

		val v = convertView
			?: inflater.inflate(R.layout.pref_key_mapping, parent, false)

		val item = this.getItem(position) ?: return v

		v.findViewById<TextView>(R.id.steno_key)
			.apply({
				this.text = item.stenoKey
			})

		v.findViewById<FlowLayout>(R.id.keyboard_keys)
			.also({
				it.removeAllViews()
				for(k in item.keyboardKeys)
				{
					val keyView = (inflater.inflate(R.layout.pref_key_mapping_key, null)
						as TextView)
					keyView.text = k
					keyView.setOnClickListener({ this.removeMapping(position, k) })
					it.addView(keyView)
				}
			})

		v.findViewById<Button>(R.id.add_keyboard_key)
			.also({
				it.setOnFocusChangeListener({ _, focused ->
					if(focused)
					{
						val toast = Toast.makeText(
							context,
							"Press a key.",
							Toast.LENGTH_SHORT
						)

						it.setOnKeyListener({ _, keyCode, _ ->
							it.clearFocus()
							it.setOnKeyListener(null)
							toast.cancel()
							this.addMapping(position, keyCodeToString(keyCode))
							true
						})

						toast.show()
					}
				})
			})

		return v
	}
}

class KeyMapPreference(context: Context, attributes: AttributeSet) :
	DialogPreference(context, attributes)
{
	var keyLayout: KeyLayout = IRELAND_LAYOUT
		set(v)
		{
			field = v

			val items = mutableListOf<KeyMapping>()
			for((i, k) in v.rtfcreKeys.withIndex())
				if(k.pure)
				{
					val keyString = (
						(if(i >= v.breakKeys.second) "-" else "")
							+ k.char.toString ()
							+ (if(i < v.breakKeys.first) "-" else ""))

					items.add(KeyMapping(keyString, mutableListOf()))
				}
			this._items = items
		}
	private var _items: List<KeyMapping> = listOf()
	var items: List<KeyMapping>
		get() = this._items
		set(v)
		{
			this._items = v
			this.persistString(v.toJson().toString())
		}

	override val dialogFragment get() = KeyMapPreferenceFragment()

	init { this.keyLayout = IRELAND_LAYOUT }

	override fun getDialogLayoutResource(): Int = R.layout.pref_key_map

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any? =
		a.getString(index)
	override fun onSetInitialValue(defaultValue: Any?)
	{
		val value = this.getPersistedString(defaultValue as? String) ?: return

		val keyMap = keyMapFromJson(this.key, value)

		for(item in this.items)
		{
			val mapping = keyMap[item.stenoKey]
			if(mapping != null)
			{
				item.keyboardKeys.clear()
				item.keyboardKeys.addAll(mapping)
			}
		}
	}
}

class KeyMapPreferenceFragment : PreferenceDialogFragmentCompat()
{
	private val items = mutableListOf<KeyMapping>()
	private var adapter: KeyMapAdapter? = null

	override fun onBindDialogView(view: View)
	{
		super.onBindDialogView(view)
		this.adapter = KeyMapAdapter(this.context!!, this.items)
		this.items.clear()
		(this.preference as? KeyMapPreference)?.items?.also({ this.items.addAll(it) })
		view.findViewById<ListView>(R.id.main).adapter = this.adapter
	}

	override fun onDialogClosed(positiveResult: Boolean)
	{
		if(positiveResult)
			(this.preference as? KeyMapPreference)?.also({ it.items = this.items })
	}
}
