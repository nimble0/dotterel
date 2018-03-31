// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.*

import com.beust.klaxon.*

import java.io.IOException
import java.io.StringReader

import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.translation.systems.IRELAND_LAYOUT

data class KeyMapping(val stenoKey: String, val keyboardKeys: MutableList<String>)

fun List<KeyMapping>.toJson(): String
{
	val values = this
	return json({
		obj(*values.map({
			it.stenoKey to JsonArray(it.keyboardKeys)
		}).toTypedArray())
	}).toJsonString()
}

fun keyMapFromJson(key: String, json: String): Map<String, List<String>>
{
	try
	{
		val keyMap = mutableMapOf<String, List<String>>()
		JsonReader(StringReader(json)).use(
			{ reader ->
				reader.beginObject()
				{
					while(reader.hasNext())
					{
						val stenoKey = reader.nextName()
						val keyboardKeys = mutableListOf<String>()
						reader.beginArray()
						{
							while(reader.hasNext())
								keyboardKeys.add(reader.nextString())
						}
						keyMap[stenoKey] = keyboardKeys
					}
				}
			})

		return keyMap
	}
	catch(e: KlaxonException)
	{
		Log.e("Preferences", "Preference $key has badly formed JSON")
	}
	catch(e: IllegalStateException)
	{
		Log.e("Preferences", "Invalid type found while reading preference $key")
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
		val item = this.getItem(i)
		if(keyCode !in item.keyboardKeys)
			item.keyboardKeys.add(keyCode)
		this.notifyDataSetChanged()
	}
	private fun removeMapping(i: Int, keyCode: String)
	{
		val item = this.getItem(i)
		item.keyboardKeys.remove(keyCode)
		this.notifyDataSetChanged()
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup)
		: View
	{
		val inflater = LayoutInflater.from(this.context)

		val v = convertView
			?: inflater.inflate(R.layout.pref_key_mapping, parent, false)

		val item = this.getItem(position)

		v.findViewById<TextView>(R.id.steno_key)
			.apply({
				this.text = item.stenoKey
			})

		val _this = this

		v.findViewById<FlowLayout>(R.id.keyboard_keys)
			.apply({
				this.removeAllViews()
				for(k in item.keyboardKeys)
				{
					val keyView = (inflater.inflate(
						R.layout.pref_key_mapping_key,
						null
					) as TextView)
						.apply({
							this.text = k
							this.setOnClickListener({ _this.removeMapping(position, k) })
						})
					this.addView(keyView)
				}
			})

		v.findViewById<Button>(R.id.add_keyboard_key)
			.apply({
				this.setOnFocusChangeListener({ _, focused ->
					if(focused)
					{
						val toast = Toast.makeText(
							context,
							"Press a key.",
							Toast.LENGTH_SHORT
						)

						this.setOnKeyListener({ _, keyCode, _ ->
							this.clearFocus()
							this.setOnKeyListener(null)
							toast.cancel()
							_this.addMapping(position, keyCodeToString(keyCode))
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
	var keyLayout: KeyLayout = KeyLayout("", "", "", mapOf())
		set(v)
		{
			field = v
			for((i, k) in v.rtfcreKeys.withIndex())
			{
				if(k.pure)
				{
					val keyString = (
						(if(i >= v.breakKeys.second) "-" else "")
						+ k.char.toString ()
						+ (if(i < v.breakKeys.first) "-" else ""))

					items.add(KeyMapping(keyString, mutableListOf()))
				}
			}
		}

	private val items = mutableListOf<KeyMapping>()
	private var adapter = KeyMapAdapter(context, this.items)
	init
	{
		this.dialogLayoutResource = R.layout.pref_key_map
		this.setPositiveButtonText(android.R.string.ok)
		this.setNegativeButtonText(android.R.string.cancel)

		this.dialogIcon = null

		this.keyLayout = IRELAND_LAYOUT
	}

	override fun onBindDialogView(view: View?)
	{
		super.onBindDialogView(view)
		this.load(this.getPersistedString("{}"))
		(view as ListView).adapter = this.adapter
	}

	override fun onGetDefaultValue(array: TypedArray, i: Int): Any =
		array.getString(i)

	override fun onSetInitialValue(restore: Boolean, defaultValue: Any?)
	{
		this.load(
			if(restore)
				this.getPersistedString("{}")
			else
				defaultValue as? String ?: "{}")
		this.save()
	}

	override fun onDialogClosed(positiveResult: Boolean)
	{
		if(positiveResult)
			this.save()
	}

	fun save() = this.persistString(this.items.toJson())

	fun load(value: String)
	{
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
		this.adapter.notifyDataSetChanged()
	}
}
