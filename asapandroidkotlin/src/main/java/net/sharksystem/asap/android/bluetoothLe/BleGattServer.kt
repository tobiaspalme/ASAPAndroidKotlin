package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.sharksystem.asap.android.util.getLogStart
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID


/**
 * Represents a Bluetooth Low Energy (BLE) GATT server.
 *
 * When a client reads the characteristic, the server responds with its L2CAP Protocol/Service Multiplexer (PSM) value.
 * This PSM value is then used by the client to establish an L2CAP connection for data transfer.
 *
 * @property context application context
 * @property bleSocketConnectionListener A listener to be notified about successful connection
 * @property serviceUUID UUID of the GATT service to be offered by this server
 * @property characteristicUUID UUID of the GATT characteristic within the service
 * @property bluetoothManager BluetoothManager instance
 * @property adapter BluetoothAdapter instance
 * @property advertiser BluetoothLeAdvertiser instance
 * @property scope CoroutineScope, defaulting to `CoroutineScope(Dispatchers.IO)`
 */
@SuppressLint("MissingPermission")
class BleGattServer(
    private val context: Context,
    private val bleSocketConnectionListener: BleSocketConnectionListener,
    private val serviceUUID: UUID,
    private val characteristicUUID: UUID,
    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java),
    private val adapter: BluetoothAdapter = bluetoothManager.adapter,
    private val advertiser: BluetoothLeAdvertiser? = bluetoothManager.adapter.bluetoothLeAdvertiser,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private var gattServer: BluetoothGattServer? = null

    private lateinit var serverSocket: BluetoothServerSocket

    private lateinit var gattServerJob2: Job

    private val connectedDevices: MutableList<BluetoothDevice> = mutableListOf()


    /**
     * Starts the GATT server, advertising, and listening for incoming connections.
     */
    fun start() {
        startGattServer()
        startAdvertising()
        startListenUsingServerSocket()
    }

    /**
     * Stops the GATT server and advertising.
     */
    fun stop() {
        advertiser?.stopAdvertising(GattServerAdvertiseCallback)
        connectedDevices.forEach { gattServer?.cancelConnection(it) }
        gattServer?.close()
        gattServer = null
        gattServerJob2.cancel()
    }

    private fun startGattServer() {
        Log.d(this.getLogStart(), "Opening GATT server...")
        val service =
            BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).also {
                it.addCharacteristic(
                    BluetoothGattCharacteristic(
                        characteristicUUID,
                        BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ,
                    )
                )
            }
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        gattServer?.addService(service)
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(serviceUUID))
            .build()

        Log.d(this.getLogStart(), "Start advertising")
        if (advertiser == null) {
            Log.e(this.getLogStart(), "Advertiser is null")
        } else {
            advertiser.startAdvertising(settings, data, GattServerAdvertiseCallback)
        }
    }

    private fun startListenUsingServerSocket() {
        Log.d(this.getLogStart(), "Start Listening")
        serverSocket = adapter.listenUsingInsecureL2capChannel()

        gattServerJob2 = scope.launch {
            while (true) {
                try {
                    val socket = serverSocket.accept()
                    Log.d(this.getLogStart(), "Server accepted connection -> handle Encounter")
                    bleSocketConnectionListener.onSuccessfulConnection(socket, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int,
        ) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Log.i(
                        this.getLogStart(),
                        "----> Server: ${device.name} ${device.address} connected to gatt server"
                    )
                    connectedDevices.add(device)
                }

                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.i(
                        this.getLogStart(),
                        "----> Server: ${device.name} ${device.address} disconnected from gatt server"
                    )
                    connectedDevices.remove(device)
                }
            }
            Log.d(this.getLogStart(), "connectedDevices: $connectedDevices")
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?,
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

            Log.d(
                this.getLogStart(),
                "Characteristic Read request characteristic: ${characteristic?.uuid}"
            )
            val psmByteArray = ByteBuffer
                .allocate(Int.SIZE_BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(serverSocket.psm)
                .array()

            gattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                psmByteArray,
            )
        }
    }

    private object GattServerAdvertiseCallback : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(this.getLogStart(), "Started advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(this.getLogStart(), "Failed to start advertising: $errorCode")
        }
    }

}