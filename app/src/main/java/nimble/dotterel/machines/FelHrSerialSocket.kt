// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection

import com.felhr.usbserial.CDCSerialDevice
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

private val felHr_DATA_BITS = mapOf(
	Pair(DataBits._5, UsbSerialInterface.DATA_BITS_5),
	Pair(DataBits._6, UsbSerialInterface.DATA_BITS_6),
	Pair(DataBits._7, UsbSerialInterface.DATA_BITS_7),
	Pair(DataBits._8, UsbSerialInterface.DATA_BITS_8)
)

private val felHr_STOP_BITS = mapOf(
	Pair(StopBits._1, UsbSerialInterface.STOP_BITS_1),
	Pair(StopBits._1_5, UsbSerialInterface.STOP_BITS_15),
	Pair(StopBits._2, UsbSerialInterface.STOP_BITS_2)
)

private val felHr_PARITY = mapOf(
	Pair(Parity.NONE, UsbSerialInterface.PARITY_NONE),
	Pair(Parity.ODD, UsbSerialInterface.PARITY_ODD),
	Pair(Parity.EVEN, UsbSerialInterface.PARITY_EVEN),
	Pair(Parity.MARK, UsbSerialInterface.PARITY_MARK),
	Pair(Parity.SPACE, UsbSerialInterface.PARITY_SPACE)
)

private val felHr_FLOW_CONTROL = mapOf(
	Pair(FlowControl.OFF, UsbSerialInterface.FLOW_CONTROL_OFF),
	Pair(FlowControl.RTS_CTS, UsbSerialInterface.FLOW_CONTROL_RTS_CTS),
	Pair(FlowControl.DSR_DTR, UsbSerialInterface.FLOW_CONTROL_DSR_DTR),
	Pair(FlowControl.XON_XOFF, UsbSerialInterface.FLOW_CONTROL_XON_XOFF)
)

class FelHrSerialSocket(
	device: UsbDevice,
	connection: UsbDeviceConnection
) :
	SerialSocket
{
	private val device: UsbSerialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection)
		?: throw Exception("Error creating USB serial device")

	init
	{
		if(!this.device.open())
		{
			if(this.device is CDCSerialDevice)
				throw Exception("Error creating USB serial device (CDC device)")
			else
				throw Exception("Error creating USB serial device (non-CDC device")
		}

		this.device.setDTR(true)
		this.device.setRTS(true)

		this.device.read({ this.receive(it) })
	}

	private fun receive(data: ByteArray) = this.protocol?.receive(data)

	override var baudRate: Int = 9200
		set(v)
		{
			field = v
			this.device.setBaudRate(v)
		}
	override var dataBits: DataBits = DataBits._8
		set(v)
		{
			field = v
			this.device.setDataBits(felHr_DATA_BITS[v]!!)
		}
	override var stopBits: StopBits = StopBits._1
		set(v)
		{
			field = v
			this.device.setStopBits(felHr_STOP_BITS[v]!!)
		}
	override var parity: Parity = Parity.NONE
		set(v)
		{
			field = v
			this.device.setParity(felHr_PARITY[v]!!)
		}
	override var flowControl: FlowControl = FlowControl.OFF
		set(v)
		{
			field = v
			this.device.setParity(felHr_FLOW_CONTROL[v]!!)
		}

	override var protocol: SerialProtocol? = null

	override fun send(data: ByteArray) = this.device.write(data)
	override fun close() = this.device.close()
}
