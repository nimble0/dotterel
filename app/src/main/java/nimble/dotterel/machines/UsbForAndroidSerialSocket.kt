// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.machines

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.util.Log

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager

import java.util.concurrent.Executors

private val usbForAndroid_DATA_BITS = mapOf(
	Pair(DataBits._5, UsbSerialPort.DATABITS_5),
	Pair(DataBits._6, UsbSerialPort.DATABITS_6),
	Pair(DataBits._7, UsbSerialPort.DATABITS_7),
	Pair(DataBits._8, UsbSerialPort.DATABITS_8)
)

private val usbForAndroid_STOP_BITS = mapOf(
	Pair(StopBits._1, UsbSerialPort.STOPBITS_1),
	Pair(StopBits._1_5, UsbSerialPort.STOPBITS_1_5),
	Pair(StopBits._2, UsbSerialPort.STOPBITS_2)
)

private val usbForAndroid_PARITY = mapOf(
	Pair(Parity.NONE, UsbSerialPort.PARITY_NONE),
	Pair(Parity.ODD, UsbSerialPort.PARITY_ODD),
	Pair(Parity.EVEN, UsbSerialPort.PARITY_EVEN),
	Pair(Parity.MARK, UsbSerialPort.PARITY_MARK),
	Pair(Parity.SPACE, UsbSerialPort.PARITY_SPACE)
)

//private val usbForAndroid_FLOW_CONTROL = mapOf(
//	Pair(FlowControl.OFF, UsbSerialPort.FLOWCONTROL_NONE),
//	Pair(FlowControl.RTS_CTS, UsbSerialPort.FLOWCONTROL_RTSCTS_IN),
//	Pair(FlowControl.DSR_DTR, UsbSerialPort.FLOWCONTROL_RTSCTS_OUT),
//	Pair(FlowControl.XON_XOFF, UsbSerialPort.FLOWCONTROL_XONXOFF_OUT)
//)

private val prober = UsbSerialProber(ProbeTable().also({
	it.addProduct(0xFEED, 0x1337, CdcAcmSerialDriver::class.java)
}))

class UsbForAndroidSerialSocket(
	device: UsbDevice,
	connection: UsbDeviceConnection
) :
	SerialSocket
{
	private val usbSerialPort: UsbSerialPort
	private var isOpen = false
	private var serialIoManager: SerialInputOutputManager

	init
	{
		val driver = prober.probeDevice(device)
			?: throw Exception("Error probing serial USB device $device")
		this.usbSerialPort = driver.ports[0]
		this.usbSerialPort.open(connection)
		this.isOpen = true

		this.usbSerialPort.dtr = true
		this.usbSerialPort.rts = true

		this.serialIoManager = SerialInputOutputManager(
			this.usbSerialPort,
			object : SerialInputOutputManager.Listener
			{
				override fun onRunError(e: Exception)
				{
					Log.d("Dotterel Serial", "Runner stopped ${e.message}")
				}

				override fun onNewData(data: ByteArray)
				{
					this@UsbForAndroidSerialSocket.protocol?.receive(data)
				}
			})
		Executors.newSingleThreadExecutor().submit(this.serialIoManager)
	}

	override var baudRate: Int = BAUDRATE_DEFAULT
		set(v)
		{
			this.usbSerialPort.setParameters(
				v,
				usbForAndroid_DATA_BITS[this.dataBits] ?: 8,
				usbForAndroid_STOP_BITS[this.stopBits] ?: 1,
				usbForAndroid_PARITY[this.parity] ?: UsbSerialPort.PARITY_NONE)
		}
	override var dataBits: DataBits = DataBits.DEFAULT
		set(v)
		{
			this.usbSerialPort.setParameters(
				this.baudRate,
				usbForAndroid_DATA_BITS[v] ?: 8,
				usbForAndroid_STOP_BITS[this.stopBits] ?: 1,
				usbForAndroid_PARITY[this.parity] ?: UsbSerialPort.PARITY_NONE)
		}
	override var stopBits: StopBits = StopBits.DEFAULT
		set(v)
		{
			this.usbSerialPort.setParameters(
				this.baudRate,
				usbForAndroid_DATA_BITS[this.dataBits] ?: 8,
				usbForAndroid_STOP_BITS[v] ?: 1,
				usbForAndroid_PARITY[this.parity] ?: UsbSerialPort.PARITY_NONE)
		}
	override var parity: Parity = Parity.DEFAULT
		set(v)
		{
			this.usbSerialPort.setParameters(
				this.baudRate,
				usbForAndroid_DATA_BITS[this.dataBits] ?: 8,
				usbForAndroid_STOP_BITS[this.stopBits] ?: 1,
				usbForAndroid_PARITY[v] ?: UsbSerialPort.PARITY_NONE)
		}
	override var flowControl: FlowControl = FlowControl.DEFAULT
		set(v)
		{
		}
	override var protocol: SerialProtocol? = null

	override fun send(data: ByteArray)
	{
		this.usbSerialPort.write(data, 10)
	}

	override fun close()
	{
		if(this.isOpen)
		{
			this.serialIoManager.stop()
			this.usbSerialPort.close()
			this.isOpen = false
		}
	}
}
