// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.*

import androidx.preference.*

import com.eclipsesource.json.*

import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.util.*
import nimble.dotterel.util.DialogPreference

data class KeyMapping(val stenoKey: String, val keyboardKeys: MutableList<String>)

fun keyCodeToString(keyCode: Int) =
	KeyEvent.keyCodeToString(keyCode).substring("KEYCODE_".length).toLowerCase()

fun stringToKeyCode(keyCodeString: String) =
	KeyEvent.keyCodeFromString("KEYCODE_${keyCodeString.toUpperCase()}")

class KeyMapAdapter(context: Context, items: List<KeyMapping>, val readOnly: Boolean) :
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
					val keyView = (inflater.inflate(R.layout.pref_key_mapping_key, parent, false)
						as TextView)
					keyView.text = k
					keyView.isEnabled = !this.readOnly
					keyView.setOnClickListener({ this.removeMapping(position, k) })
					it.addView(keyView)
				}
			})

		v.findViewById<Button>(R.id.add_keyboard_key)
			.also({
				it.isEnabled = !this.readOnly

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
	private var keyLayout: KeyLayout = KeyLayout("#STKPWHR-AO*EU-FRPBLGTSDZ")

	val readOnly get() = (this.extras?.getBoolean("readOnly") == true)

	override val dialogFragment get() = KeyMapPreferenceFragment()

	override fun getDialogLayoutResource(): Int = R.layout.pref_key_map

	override fun setPreferenceDataStore(dataStore: PreferenceDataStore?)
	{
		super.setPreferenceDataStore(dataStore)
		this.load()
	}

	internal fun updateKeyLayout()
	{
		this.keyLayout = KeyLayout((this.preferenceDataStore as? JsonDataStore)
			?.safeGet("keyLayout", null, { v -> v.asObject() }) ?: JsonObject())
	}

	internal fun save(items: List<KeyMapping>)
	{
		if(this.readOnly)
			return

		val layout = items.let({ mappings: List<KeyMapping> ->
			val keyMap = JsonObject()
			for(mapping in mappings)
				keyMap.add(mapping.stenoKey, mapping.keyboardKeys.toJson())
			keyMap
		})

		(this.preferenceDataStore as? JsonDataStore)?.safePut(this.key, layout)
	}

	internal fun load(): List<KeyMapping>
	{
		val keyMap = (this.preferenceDataStore as? JsonDataStore)
			?.safeGet(
				this.key,
				null,
				{ v ->
					v.asObject()
						.mapValues({
							it.value.asArray().map({ key -> key.asString() })
						})
				})
			?: mapOf()

		val items = this.keyLayout.pureKeysList.map({ KeyMapping(it, mutableListOf()) })
		for(item in items)
		{
			val mapping = keyMap[item.stenoKey]
			if(mapping != null)
			{
				item.keyboardKeys.clear()
				item.keyboardKeys.addAll(mapping)
			}
		}

		return items
	}
}

class KeyMapPreferenceFragment : PreferenceDialogFragmentCompat()
{
	private var items: List<KeyMapping> = listOf()

	override fun onBindDialogView(view: View)
	{
		super.onBindDialogView(view)

		val preference = (this.preference as? KeyMapPreference)!!
		preference.updateKeyLayout()
		this.items = preference.load()
		view.findViewById<ListView>(R.id.main).adapter = KeyMapAdapter(
			this.requireContext(),
			this.items,
			preference.readOnly)
	}

	override fun onDialogClosed(positiveResult: Boolean)
	{
		if(positiveResult)
			(this.preference as? KeyMapPreference)?.save(this.items)
	}
}
