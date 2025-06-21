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
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
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

        const val ACTION_START_ADVERTISING = "start_ad"
        const val ACTION_STOP_ADVERTISING = "stop_ad"

        val isServerRunning = MutableStateFlow(false)

        private const val CHANNEL = "gatt_server_channel"

        private var asapEncounterManager: ASAPEncounterManager? = null

        fun setASAPEncounterManager(manager: ASAPEncounterManager) {
            asapEncounterManager = manager
        }

        fun getASAPEncounterManager(): ASAPEncounterManager? {
            return asapEncounterManager
        }
    }

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
        // If we are missing permission stop the service
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        if (permission == PackageManager.PERMISSION_GRANTED) {
            startInForeground()

            Log.d(this.getLogStart(), "Opening GATT server...")
            server = manager.openGattServer(applicationContext, GattServerCallback())
            Thread.sleep(1000)
            server.addService(service)
            isServerRunning.value = true
        } else {
            Log.d(this.getLogStart(), "Missing connect permission")
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || !hasAdvertisingPermission()) {
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_START_ADVERTISING -> {
                Log.d(this.getLogStart(), "Start advertising")
                startAdvertising()
                serverSocket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    adapter.listenUsingInsecureL2capChannel()
                } else {
                    TODO("VERSION.SDK_INT < Q")
                }
                val byteArray = ByteArray(1)
                byteArray[0] = serverSocket.psm.toByte()
                server.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID).value =
                    byteArray
                Log.d(this.getLogStart(), "ServerSocket psm: ${serverSocket.psm}")
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    val socket = serverSocket.accept()
                    Log.d(this.getLogStart(), "-----> Server accepted connection handle Encounter <-----")
                    handleBTSocket(socket, false)
                }
            }

            ACTION_STOP_ADVERTISING -> {
                Log.d(this.getLogStart(), "Stop advertising")
                advertiser.stopAdvertising(SampleAdvertiseCallback)
            }

            else -> throw IllegalArgumentException("Unknown action")
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        isServerRunning.value = false
        if (hasAdvertisingPermission()) {
            advertiser.stopAdvertising(SampleAdvertiseCallback)
        }
        server.close()
        scope.cancel()
        Log.d(this.getLogStart(), "Server destroyed")
    }

    private fun startInForeground() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle("GATT Server")
            .setContentText("Running...")
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                100,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(100, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        )
            .setName("GATT Server channel")
            .setDescription("Channel for the GATT server sample")
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
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

        advertiser.startAdvertising(settings, data, SampleAdvertiseCallback)
    }

    @SuppressLint("MissingPermission")
    inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int,
        ) {
            Log.d(
                this.getLogStart(),
                "device: ${device.name} ${device.address} Connection change to $newState"
            )
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {


                    /*val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                    val bleStreamPair =
                        BleStreamPair(server, characteristic, device.address, device)
                    activeConnections[device] = bleStreamPair

                    // Get StreamPair and call handleEncounter
                    val streamPair = bleStreamPair.getStreamPair()
                    getASAPEncounterManager()?.handleEncounter(
                        streamPair,
                        ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK
                    )*/
                }

                BluetoothGatt.STATE_DISCONNECTED -> {

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

    object SampleAdvertiseCallback : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(this.getLogStart(), "Started advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d(this.getLogStart(), "Failed to start advertising: $errorCode")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun handleBTSocket(socket: BluetoothSocket, initiator: Boolean) {
        val remoteMacAddress = socket.remoteDevice.address

        val streamPair = StreamPairImpl.getStreamPairWithEndpointAddress(
            socket.inputStream, socket.outputStream, remoteMacAddress
        )

        getASAPEncounterManager()?.handleEncounter(
            streamPair, ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK, initiator
        )
    }

}
