// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject

import java.io.Closeable

import nimble.dotterel.Dotterel
import nimble.dotterel.StenoMachine
import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.translation.KeyMap
import nimble.dotterel.translation.Stroke
import nimble.dotterel.util.mapValues
import java.io.IOException
import nimble.dotterel.StenoMachineTracker


val UsbDevice.id: String
	get() = when
	{
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
			"${this.manufacturerName} ${this.productName} ${this.version}" +
				" (${this.vendorId.toString(16)}:${this.productId.toString(16)})" +
				" : ${this.serialNumber}"
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ->
			"${this.manufacturerName} ${this.productName}" +
				" (${this.vendorId.toString(16)}:${this.productId.toString(16)})" +
				" : ${this.serialNumber}"
		else ->
			"ID ${this.vendorId.toString(16)}:0x${this.productId.toString(16)}"
	}

val SERIAL_LIBRARIES = mapOf<String, (UsbDevice, UsbDeviceConnection) -> SerialSocket>(
	Pair(
		"felHr",
		{ device: UsbDevice, connection: UsbDeviceConnection ->
			FelHrSerialSocket(device, connection)
		}),
	Pair(
		"usb-serial-for-android",
		{ device: UsbDevice, connection: UsbDeviceConnection ->
			UsbForAndroidSerialSocket(device, connection)
		})
)

val PROTOCOLS = mapOf<String, (SerialSocket) -> StenoSerialProtocol>(
	Pair("TxBolt", { socket: SerialSocket -> TxBoltSerialProtocol(socket) }),
	Pair("GeminiPr", { socket: SerialSocket -> GeminiPrSerialProtocol(socket) })
)

val BAUDRATE_DEFAULT = 9200

enum class DataBits
{
	_5,
	_6,
	_7,
	_8;

	companion object
	{
		val DEFAULT = _8
		fun valueOf2(s: String) = DataBits.valueOf("_$s")
	}
}

enum class StopBits
{
	_1,
	_1_5,
	_2;

	companion object
	{
		val DEFAULT = _1
		fun valueOf2(s: String) = StopBits.valueOf("_${s.replace(".", "_")}")
	}
}

enum class Parity
{
	NONE,
	ODD,
	EVEN,
	MARK,
	SPACE;

	companion object
	{
		val DEFAULT = NONE
		fun valueOf2(s: String) = Parity.valueOf(s.toUpperCase())
	}
}

enum class FlowControl
{
	OFF,
	RTS_CTS,
	DSR_DTR,
	XON_XOFF;

	companion object
	{
		val DEFAULT = OFF
		fun valueOf2(s: String) = FlowControl.valueOf(s.replace("/", "_").toUpperCase())
	}
}

private val DEFAULT_SERIAL_CONFIG = Json.parse("""{
	"library": "usb-serial-for-android",
	"protocol": "TxBolt"
}""").asObject()

private const val ACTION_USB_PERMISSION = "nimble.dotterel.USB_PERMISSION"
private const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"

class SerialStenoMachine(
	private val app: Dotterel,
	val id: String
) :
	StenoMachine,
	StenoMachine.Listener,
	Dotterel.IntentListener
{
	private val usbManager: UsbManager = this.app.getSystemService(Context.USB_SERVICE) as UsbManager
	private var device: UsbDevice? = null

	private var socket: SerialSocket? = null
	private var protocol: StenoSerialProtocol? = null

	class Factory :
		StenoMachine.Factory,
		Dotterel.IntentListener
	{
		override var tracker: StenoMachineTracker? = null
			set(v)
			{
				field = v
				val tracker = v ?: return
				tracker.intentForwarder
					.add(UsbManager.ACTION_USB_DEVICE_ATTACHED, this)
				tracker.intentForwarder
					.add(UsbManager.ACTION_USB_DEVICE_DETACHED, this)

				(tracker.androidContext.getSystemService(Context.USB_SERVICE) as UsbManager)
					.deviceList
					.values
					.map({ it.id })
					.forEach({ tracker.addMachine(Pair("Serial", it)) })
			}

		override fun makeStenoMachine(app: Dotterel, id: String) = SerialStenoMachine(app, id)

		override fun onIntent(context: Context, intent: Intent)
		{
			when(intent.action)
			{
				UsbManager.ACTION_USB_DEVICE_ATTACHED ->
					this.tracker?.addMachine(Pair(
						"Serial",
						intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE).id))
				UsbManager.ACTION_USB_DEVICE_DETACHED ->
					this.tracker?.removeMachine(Pair(
						"Serial",
						intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE).id))
			}
		}
	}

	init
	{
		this.app.intentForwarder.add(ACTION_USB_DETACHED, this)
	}

	override fun setConfig(
		keyLayout: KeyLayout,
		config: JsonObject,
		systemConfig: JsonObject)
	{
		try
		{
			@Suppress("NAME_SHADOWING")
			val config = config.get(this.id)?.asObject() ?: JsonObject()

			val device = this.usbManager.deviceList?.values?.find({ it.id == this.id })!!
			this.device = device
			if(!this.usbManager.hasPermission(device))
			{
				this.requestUserPermission()
				return
			}
			else
				Log.i("Dotterel", "USB device access permission granted for ${this.id}")

			this.socket = SERIAL_LIBRARIES[config.get("library")?.asString() ?: DEFAULT_SERIAL_CONFIG.get("library").asString()]
				?.invoke(
					this.device!!,
					this.usbManager.openDevice(device)!!)
				?.also({ socket ->
					config.get("baudRate")?.also({ v -> socket.baudRate = v.asInt() })
					config.get("dataBits")?.also({ v -> socket.dataBits = DataBits.valueOf2(v.asString()) })
					config.get("stopBits")?.also({ v -> socket.stopBits = StopBits.valueOf2(v.asString()) })
					config.get("parity")?.also({ v -> socket.parity = Parity.valueOf2(v.asString()) })
					config.get("flowControl")?.also({ v -> socket.flowControl = FlowControl.valueOf2(v.asString()) })
				})

			val mapping = systemConfig["TxBolt"]!!.asObject()
				.get("layout").asObject()
				.mapValues({ it.value.asArray().map({ key -> key.asString() }) })

			this.protocol = PROTOCOLS[config.get("protocol")?.asString() ?: DEFAULT_SERIAL_CONFIG.get("protocol").asString()]
				?.invoke(this.socket!!)
				?.also({ protocol ->
					protocol.keyLayout = keyLayout
					protocol.keyMap = KeyMap(
						keyLayout,
						mapping,
						{
							val b = protocol.keys.indexOf(it)
							if(b == -1)
								null
							else
								b
						})
					protocol.strokeListener = this.strokeListener
				})
			this.protocol?.strokeListener = this
			this.socket?.protocol = this.protocol
		}
		catch(e: java.lang.NullPointerException)
		{
			throw IllegalArgumentException(e)
		}
		catch(e: java.lang.UnsupportedOperationException)
		{
			throw IllegalArgumentException(e)
		}
		catch(e: IOException)
		{
			Log.e("Dotterel", "Error opening serial socket ${this.id}: $e")
		}
	}

	override var strokeListener: StenoMachine.Listener? = null
		set(v)
		{
			field = v
			this.protocol?.strokeListener = v
		}

	override fun close()
	{
		this.socket?.close()
	}

	private fun requestUserPermission()
	{
		Log.i("Dotterel", "Requesting USB device access permission for ${this.id}")

		this.app.intentForwarder.add(ACTION_USB_PERMISSION, this)
		val pendingIntent = PendingIntent.getBroadcast(
			this.app,
			0,
			Intent(ACTION_USB_PERMISSION),
			0)
		this.usbManager.requestPermission(this.device, pendingIntent)
	}

	override fun onIntent(context: Context, intent: Intent)
	{
		when(intent.action)
		{
			ACTION_USB_PERMISSION ->
			{
				if(intent.extras?.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED) == true)
					this.app.configureMachine(Pair("Serial", this.id))
				else
				{
					val m = "USB device access permission not granted for ${this.id}"
					Log.e("Dotterel", m)
					Toast.makeText(this.app, m, Toast.LENGTH_LONG).show()
				}
			}
			ACTION_USB_DETACHED ->
			{
			}
		}
	}

	override fun applyStroke(s: Stroke)
	{
		this.strokeListener?.applyStroke(s)
		this.app.poke()
		this.app.window.window?.makeActive()
	}

	override fun changeStroke(s: Stroke)
	{
		this.strokeListener?.changeStroke(s)
	}
}

abstract class StenoSerialProtocol(override val socket: SerialSocket) : SerialProtocol
{
	abstract val keys: List<String>

	var strokeListener: StenoMachine.Listener? = null
	var keyLayout: KeyLayout = KeyLayout("")
	var keyMap: KeyMap<Int> = KeyMap(KeyLayout(""), mapOf(), { 0 })

	private fun keysToStroke(keys: Long): Stroke =
		(0 until this.keys.size).fold(
			Stroke(this.keyLayout, 0L),
			{ acc, it ->
				if(keys and (1L shl it) != 0L)
					acc + this.keyMap.parse(it)
				else
					acc
			})

	fun applyStroke(keys: Long) =
		this.strokeListener?.applyStroke(this.keysToStroke(keys))
}


private const val MAX_KEYS = 64


interface SerialSocket : Closeable
{
	var baudRate: Int
	var dataBits: DataBits
	var stopBits: StopBits
	var parity: Parity
	var flowControl: FlowControl

	var protocol: SerialProtocol?

	fun send(data: ByteArray)
	override fun close()
}

interface SerialProtocol
{
	val socket: SerialSocket

	fun receive(data: ByteArray)
}
