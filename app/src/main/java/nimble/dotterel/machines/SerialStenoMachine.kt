// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build

import com.eclipsesource.json.JsonObject
import nimble.dotterel.Dotterel

import java.io.Closeable

import nimble.dotterel.StenoMachine
import nimble.dotterel.translation.KeyLayout
import nimble.dotterel.translation.KeyMap
import nimble.dotterel.translation.Stroke

val UsbDevice.id: String
	get() = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			this.serialNumber ?: ""
		else
			"${this.vendorId}:${this.productId}"

val SERIAL_LIBRARIES = mapOf(
	Pair("felHr", { device: UsbDevice, connection: UsbDeviceConnection -> FelHrSerialSocket(device, connection) })
)

val PROTOCOLS = mapOf<String, (SerialSocket) -> StenoSerialProtocol>(
	Pair("TxBolt", { socket: SerialSocket -> TxBoltSerialProtocol(socket) }),
	Pair("GeminiPr", { socket: SerialSocket -> GeminiPrSerialProtocol(socket) })
)

enum class DataBits
{
	_5,
	_6,
	_7,
	_8
}

enum class StopBits
{
	_1,
	_1_5,
	_2
}

enum class Parity
{
	NONE,
	ODD,
	EVEN,
	MARK,
	SPACE
}

enum class FlowControl
{
	OFF,
	RTS_CTS,
	DSR_DTR,
	XON_XOFF
}

class SerialStenoMachine : StenoMachine
{
	val id: String = "Serial"

	private var usbManager: UsbManager? = null
	private var device: UsbDevice? = null

	private var socket: SerialSocket? = null
	private var protocol: StenoSerialProtocol? = null

	class Factory : StenoMachine.Factory
	{
		fun availableMachines(app: Dotterel): List<String>
		{
			val usbManager = app.getSystemService(Context.USB_SERVICE) as UsbManager

			usbManager.deviceList.keys
		}
		override fun makeStenoMachine(app: Dotterel) = NkroStenoMachine(app)
	}

	override fun setConfig(keyLayout: KeyLayout, config: JsonObject, systemConfig: JsonObject)
	{
		@Suppress("NAME_SHADOWING")
		val config = config.get(this.id).asObject()

		this.device = this.usbManager?.deviceList?.values?.find({ it.id == this.id })
		this.socket = SERIAL_LIBRARIES[config.get("library").asString()]
			?.invoke(
				this.device!!,
				this.usbManager?.openDevice(this.device!!)!!)
			?.also({
				it.baudRate = config.get("baudRate").asInt()
				it.dataBits = DataBits.valueOf("_" + config.get("dataBits").asString())
				it.stopBits = StopBits.valueOf("_" + config.get("stopBits").asString())
				it.parity = Parity.valueOf(config.get("parity").asString())
				it.flowControl = FlowControl.valueOf(config.get("flowControl").asString())
			})

		this.protocol = PROTOCOLS[config.get("protocol").asString()]?.invoke(this.socket!!)
			?.also({ it.keyLayout = keyLayout })
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
}

abstract class StenoSerialProtocol(override val socket: SerialSocket) : SerialProtocol
{
	abstract val keys: List<String>

	var strokeListener: StenoMachine.Listener? = null
	var keyLayout: KeyLayout = KeyLayout("")

	private var keyMap: KeyMap<Int> = KeyMap(KeyLayout(""), mapOf(), { 0 })

	private fun keysToStroke(keys: Long): Stroke =
		(0 until MAX_KEYS).fold(Stroke(this.keyLayout, 0L), { acc, it ->
			if(keys or (1L shl it) != 0L)
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

	val protocol: SerialProtocol?

	fun send(data: ByteArray)
	override fun close()
}

interface SerialProtocol
{
	val socket: SerialSocket

	fun receive(data: ByteArray)
}
