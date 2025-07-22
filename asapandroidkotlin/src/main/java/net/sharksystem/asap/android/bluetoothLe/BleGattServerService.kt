package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.app.Service
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
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.sharksystem.asap.android.util.getLogStart
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class BleGattServerService : Service() {

    private val binder = BleGattServerBinder()

    private val manager: BluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val adapter: BluetoothAdapter
        get() = manager.adapter

    private val advertiser: BluetoothLeAdvertiser
        get() = manager.adapter.bluetoothLeAdvertiser

    private var serviceUUID: UUID? = null

    private var characteristicUUID: UUID? = null

    private lateinit var server: BluetoothGattServer

    private lateinit var serverSocket: BluetoothServerSocket

    private lateinit var bleSocketConnectionListener: BleSocketConnectionListener

    private val connectedDevices: MutableList<BluetoothDevice> = mutableListOf()

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder {
        if (intent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                serviceUUID = intent.getSerializableExtra(BleEngine.SERVICE_UUID, UUID::class.java)
                characteristicUUID =
                    intent.getSerializableExtra(BleEngine.CHARACTERISTIC_UUID, UUID::class.java)
            } else {
                serviceUUID = intent.getSerializableExtra(BleEngine.SERVICE_UUID) as UUID
                characteristicUUID =
                    intent.getSerializableExtra(BleEngine.CHARACTERISTIC_UUID) as UUID
            }
        }
        startGattServer()

        return binder
    }

    override fun onDestroy() {
        super.onDestroy()

        advertiser.stopAdvertising(GattServerAdvertiseCallback)
        connectedDevices.forEach { server.cancelConnection(it) }
        server.close()
        scope.cancel()
        Log.d(this.getLogStart(), "Server destroyed")
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
        server = manager.openGattServer(applicationContext, gattServerCallback)
        server.addService(service)
        startAdvertising()
    }

    private fun startListenUsingServerSocket() {
        Log.d(this.getLogStart(), "Start Listening")
        serverSocket = adapter.listenUsingInsecureL2capChannel()
        Log.d(this.getLogStart(), "PSM: ${serverSocket.psm}")

        scope.launch {
            while(true){
                val socket = serverSocket.accept()
                Log.d(this.getLogStart(), "Server accepted connection -> handle Encounter")
                bleSocketConnectionListener.onSuccessfulConnection(socket, false)
            }
        }
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
        advertiser.startAdvertising(settings, data, GattServerAdvertiseCallback)
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int,
        ) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Log.d(
                        this.getLogStart(),
                        "${device.name} ${device.address} connected to gatt server"
                    )
                    connectedDevices.add(device)
                }

                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.d(
                        this.getLogStart(),
                        "${device.name} ${device.address} disconnected from gatt server"
                    )
                    connectedDevices.remove(device)
                }
            }
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

            server.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                psmByteArray,
            )
        }
    }

    object GattServerAdvertiseCallback : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(this.getLogStart(), "Started advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(this.getLogStart(), "Failed to start advertising: $errorCode")
        }
    }

    inner class BleGattServerBinder : Binder() {
        fun getService() = this@BleGattServerService
    }

    fun setBleSocketConnectionListener(bleSocketConnectionListener: BleSocketConnectionListener) {
        this.bleSocketConnectionListener = bleSocketConnectionListener
        startListenUsingServerSocket()
    }
}
