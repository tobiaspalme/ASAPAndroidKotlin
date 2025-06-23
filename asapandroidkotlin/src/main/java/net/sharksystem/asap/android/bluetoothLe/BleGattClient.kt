package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
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
class BleGattClient(
    private val context: Context,
    private val device: BluetoothDevice,
    private val asapEncounterManager: ASAPEncounterManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    var isDisconnected: Boolean = false
    private var gatt: BluetoothGatt? = null

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(this.getLogStart(), "Connected to GATT server")
                    this@BleGattClient.gatt = gatt
                    gatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(this.getLogStart(), "Disconnected from GATT server")
                    isDisconnected = true
                    this@BleGattClient.gatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(this.getLogStart(), "onServicesDiscovered: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val characteristic = gatt?.getService(BleGattServerService.SERVICE_UUID)
                    ?.getCharacteristic(BleGattServerService.CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    gatt.readCharacteristic(characteristic)
                } else {
                    Log.e(this.getLogStart(), "Characteristic not found")
                }
            }else{
                Log.e(this.getLogStart(), "onServicesDiscovered not successful: $status")
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

            val socket = device.createInsecureL2capChannel(readResult)
            coroutineScope.launch {
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
        Log.d(this.getLogStart(), "Connecting to device: ${device.name} ${device.address}")
        gatt = device.connectGatt(context, false, bluetoothGattCallback)
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
}