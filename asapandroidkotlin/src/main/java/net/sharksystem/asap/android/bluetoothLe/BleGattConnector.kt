package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getLogStart
import net.sharksystem.utils.streams.StreamPairImpl
import java.nio.ByteBuffer
import java.nio.ByteOrder

@SuppressLint("MissingPermission")
class BleGattConnector(
    private val context: Context,
    private val device: BluetoothDevice,
    private val asapEncounterManager: ASAPEncounterManager
) {
    private var gatt: BluetoothGatt? = null
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(
                this.getLogStart(),
                "device: ${device.name} ${device.address} Connection change to $newState"
            )
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    this@BleGattConnector.gatt = gatt
                    Thread.sleep(1000)
                    gatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    this@BleGattConnector.gatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onServicesDiscovered: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val characteristic = gatt?.getService(BleGattServerService.SERVICE_UUID)
                    ?.getCharacteristic(BleGattServerService.CHARACTERISTIC_UUID)
                gatt?.readCharacteristic(characteristic)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            val readResult = ByteBuffer.wrap(value)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt()
            Log.d(this.getLogStart(), "Characteristic read PSM: $readResult")

            val socket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                device.createInsecureL2capChannel(readResult)
            } else {
                TODO("VERSION.SDK_INT < Q")
            }
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                socket.connect()
                Log.d(
                    this.getLogStart(),
                    "-----> Client accepted connection handle Encounter <-----"
                )
                handleBTSocket(socket, true)
            }
        }
    }

    init {
        Log.d(TAG, "Connecting to device: ${device.address}")
        gatt = device.connectGatt(context, false, bluetoothGattCallback)
    }

    fun disconnect() {
        gatt?.close()
        gatt = null
    }

    fun handleBTSocket(socket: BluetoothSocket, initiator: Boolean) {
        val remoteMacAddress = socket.remoteDevice.address

        val streamPair = StreamPairImpl.getStreamPairWithEndpointAddress(
            socket.inputStream, socket.outputStream, remoteMacAddress
        )

        asapEncounterManager.handleEncounter(
            streamPair, ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK, initiator
        )
    }

    companion object {
        private const val TAG = "BleGattConnector"
    }
}