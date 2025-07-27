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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.sharksystem.asap.android.util.getLogStart
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID


/**
 * Manages the Bluetooth Low Energy (BLE) GATT client connection to a remote device
 *
 * @param context application context
 * @param device The [BluetoothDevice] to connect to
 * @param serviceUUID UUID of the GATT service to interact with
 * @param characteristicUUID UUID of the GATT characteristic to read the PSM from
 * @param bleSocketConnectionListener A listener to be notified about successful connection
 * @param coroutineScope CoroutineScope, defaults to `CoroutineScope(Dispatchers.IO)`
 */
@SuppressLint("MissingPermission")
class BleGattClient(
    context: Context,
    val device: BluetoothDevice,
    private val serviceUUID: UUID,
    private val characteristicUUID: UUID,
    private val bleSocketConnectionListener: BleSocketConnectionListener,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    var isDisconnected: Boolean = false
    var bluetoothSocket: BluetoothSocket? = null

    private var gatt: BluetoothGatt? = null

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(this.getLogStart(), "---> Client: Successfully connected to ${device.name}.")

                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(
                        this.getLogStart(),
                        "----> Client: Disconnected from ${device.name} with error status $status"
                    )
                    isDisconnected = true
                    this@BleGattClient.gatt = null
                } else {
                    Log.d(
                        this.getLogStart(),
                        "----> Client: Cleanly disconnected from ${device.name}"
                    )
                    isDisconnected = true
                    this@BleGattClient.gatt = null
                }

                try {
                    gatt?.close()
                } catch (e: Exception) {
                    Log.w(this.getLogStart(), "Error closing GATT: ${e.message}")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(this.getLogStart(), "onServicesDiscovered: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val characteristic = gatt?.getService(serviceUUID)
                    ?.getCharacteristic(characteristicUUID)
                if (characteristic != null) {
                    gatt.readCharacteristic(characteristic)
                } else {
                    Log.e(this.getLogStart(), "Characteristic not found")
                }
            } else {
                Log.e(this.getLogStart(), "onServicesDiscovered not successful: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            createL2CapChannel(value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            characteristic?.value?.let {
                createL2CapChannel(it)
            }
        }
    }

    private fun createL2CapChannel(psm: ByteArray) {
        val readResult = ByteBuffer.wrap(psm)
            .order(ByteOrder.BIG_ENDIAN)
            .getInt()
        Log.d(this.getLogStart(), "Characteristic read PSM: $readResult")

        bluetoothSocket = device.createInsecureL2capChannel(readResult)
        coroutineScope.launch {
            try {
                bluetoothSocket?.connect()
                Log.d(
                    this.getLogStart(),
                    "Client started connection -> handle Encounter"
                )
                bleSocketConnectionListener.onSuccessfulConnection(bluetoothSocket!!, true)
            } catch (e: Exception) {
                Log.e(this.getLogStart(), "L2CAP connection failed: ${e.message}")
                closeGatt()
            }
        }
    }

    fun closeGatt() {
        coroutineScope.launch {
            bluetoothSocket?.close()
            bluetoothSocket == null

            gatt?.disconnect()
            delay(300)

            gatt?.close()
            gatt == null
            isDisconnected = true
        }
    }

    init {
        Log.d(this.getLogStart(), "Connecting to device: ${device.name} ${device.address}")
        gatt = device.connectGatt(context, false, bluetoothGattCallback)
    }
}