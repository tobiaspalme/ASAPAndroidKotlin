package net.sharksystem.asap.android.bluetoothLe

import android.Manifest
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
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getLogStart
import net.sharksystem.utils.streams.StreamPairImpl
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class BleGattServerService : Service() {

    companion object {

        val SERVICE_UUID: UUID = UUID.fromString("00002222-0000-1000-8000-00805f9b34fb")

        // Same as the service but for the characteristic
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb")

        private var asapEncounterManager: ASAPEncounterManager? = null

        fun setASAPEncounterManager(manager: ASAPEncounterManager) {
            asapEncounterManager = manager
        }

        fun getASAPEncounterManager(): ASAPEncounterManager? {
            return asapEncounterManager
        }
    }

    private val binder = BleGattServerBinder()

    private val manager: BluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val adapter: BluetoothAdapter
        get() = manager.adapter
    private val advertiser: BluetoothLeAdvertiser
        get() = manager.adapter.bluetoothLeAdvertiser

    private val service =
        BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).also {
            it.addCharacteristic(
                BluetoothGattCharacteristic(
                    CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ,
                )
            )
        }

    private lateinit var server: BluetoothGattServer

    private lateinit var serverSocket: BluetoothServerSocket

    private val scope = CoroutineScope(SupervisorJob())


    override fun onCreate() {
        super.onCreate()
        Log.d(this.getLogStart(), "onCreate")
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        if (permission == PackageManager.PERMISSION_GRANTED) {

            Log.d(this.getLogStart(), "Opening GATT server...")
            server = manager.openGattServer(applicationContext, GattServerCallback())
            server.addService(service)
        } else {
            Log.d(this.getLogStart(), "Missing connect permission")
            stopSelf()
        }
        startAdvertisingAndListen()
    }

    fun startAdvertisingAndListen() {
        Log.d(this.getLogStart(), "Start advertising")
        startAdvertising()
        serverSocket = adapter.listenUsingInsecureL2capChannel()
        val byteArray = ByteArray(1)
        byteArray[0] = serverSocket.psm.toByte()
        server.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID).value =
            byteArray
        Log.d(this.getLogStart(), "ServerSocket psm: ${serverSocket.psm}")
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val socket = serverSocket.accept()
            Log.d(
                this.getLogStart(),
                "-----> Server accepted connection handle Encounter <-----"
            )
            handleBTSocket(socket, false)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if (hasAdvertisingPermission()) {
            advertiser.stopAdvertising(GattServerAdvertiseCallback)
        }
        server.close()
        scope.cancel()
        Log.d(this.getLogStart(), "Server destroyed")
    }

    private fun hasAdvertisingPermission() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        ) == PackageManager.PERMISSION_GRANTED)

    @SuppressLint("InlinedApi")
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        advertiser.startAdvertising(settings, data, GattServerAdvertiseCallback)
    }

    @SuppressLint("MissingPermission")
    inner class GattServerCallback : BluetoothGattServerCallback() {
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
                }

                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.d(
                        this.getLogStart(),
                        "${device.name} ${device.address} disconnected from gatt server"
                    )
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
            Log.d(this.getLogStart(), "Characteristic Read request: $requestId (offset $offset)")
            val byteArray = ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.BIG_ENDIAN)
                .putInt(serverSocket.psm).array()
            server.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                byteArray,
            )
        }
    }

    object GattServerAdvertiseCallback : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(this.getLogStart(), "Started advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d(this.getLogStart(), "Failed to start advertising: $errorCode")
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun handleBTSocket(socket: BluetoothSocket, initiator: Boolean) {
        val remoteMacAddress = socket.remoteDevice.address

        val streamPair = StreamPairImpl.getStreamPairWithEndpointAddress(
            socket.inputStream, socket.outputStream, remoteMacAddress
        )

        getASAPEncounterManager()?.handleEncounter(
            streamPair, ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK, initiator
        )
    }

    inner class BleGattServerBinder : Binder() {
        fun getService() = this@BleGattServerService
    }

}
