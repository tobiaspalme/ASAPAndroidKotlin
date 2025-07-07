package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getFormattedTimestamp
import net.sharksystem.asap.android.util.getLogStart
import net.sharksystem.asap.android.util.hasRequiredBluetoothPermissions
import java.util.UUID

@SuppressLint("MissingPermission")
class BleEngine(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
    private val serviceUUID: UUID = UUID.fromString("00002657-0000-1000-8000-00805f9b34fb"),
    private val characteristicUUID: UUID = UUID.fromString("00004923-0000-1000-8000-00805f9b34fb"),
    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter,
    private val bleSocketHandler: BleSocketHandler = BleSocketHandler(asapEncounterManager)
) {
    private var bleGattServerService: BleGattServerService? = null

    private val bleScanner: BleScanner =
        BleScanner(bluetoothAdapter!!, serviceUUID, ::onDeviceFound)

    private val openConnections: MutableMap<String, BleGattClient> = mutableMapOf()

    private val mutex = Mutex()

    fun start() {
        Log.d(this.getLogStart(), "Starting BleEngine")
        if (context.hasRequiredBluetoothPermissions()) {
            setup()
        } else {
            Log.e(this.getLogStart(), "Bluetooth permissions not granted")
        }
    }

    fun stop() {
        Log.d(this.getLogStart(), "Stopping BleEngine")
        shutdown()
    }

    private fun setup() {
        if (bluetoothAdapter == null) {
            Log.e(this.getLogStart(), "Device does not support bluetooth ")
            return
        }
        if (bluetoothAdapter.isEnabled.not()) {
            Log.e(this.getLogStart(), "Bluetooth is currently disabled")
            return
        }
        startGattServer()
        startScanner()
    }

    private fun shutdown() {
        bleScanner.stopScan()
        stopGattServer()
        openConnections.clear()
    }

    private fun startGattServer() {
        val intent = Intent(context, BleGattServerService::class.java)
        intent.putExtra(SERVICE_UUID, serviceUUID)
        intent.putExtra(CHARACTERISTIC_UUID, characteristicUUID)

        context.bindService(intent, bleGattServerConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopGattServer() {
        context.unbindService(bleGattServerConnection)
    }

    private fun startScanner() {
        bleScanner.startScan()
    }

    private suspend fun onDeviceFound(device: BluetoothDevice) {
        mutex.withLock {
            val isConnectionAlreadyEstablished = isConnectionAlreadyEstablished(device.address)
            val shouldCreateConnectionToPeer = asapEncounterManager.shouldCreateConnectionToPeer(
                device.address,
                ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK
            )

            if (isConnectionAlreadyEstablished.not() && shouldCreateConnectionToPeer) {
                val bleGattClient = BleGattClient(
                    context,
                    device,
                    serviceUUID,
                    characteristicUUID,
                    bleSocketHandler,
                )
                openConnections[device.address] = bleGattClient
                logState.value += "[${getFormattedTimestamp()}] Device: ${device.name} ${device.address} isConnected: true\n"
            }
        }
    }

    private fun isConnectionAlreadyEstablished(macAddress: String): Boolean {
        val bleGattConnector = openConnections[macAddress]
        if (bleGattConnector != null) {
            if (bleGattConnector.isDisconnected) {
                Log.d(this.getLogStart(), "Connection not established anymore to $macAddress")
                openConnections.remove(macAddress)
                logState.value += "[${getFormattedTimestamp()}] Device: ${macAddress} isConnected: false\n"
                return false
            } else {
                Log.d(this.getLogStart(), "Connection already established to $macAddress")
                return true
            }
        } else {
            return false
        }
    }

    private val bleGattServerConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            Log.d(this.getLogStart(), "Service connected")
            val binder = service as BleGattServerService.BleGattServerBinder
            bleGattServerService = binder.getService()
            bleGattServerService?.setSuccessfulBleConnectionListener(bleSocketHandler)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(this.getLogStart(), "Service disconnected")
        }

    }

    companion object {
        const val SERVICE_UUID = "serviceUuid"
        const val CHARACTERISTIC_UUID = "characteristicUuid"

        // only used for demonstration purpose
        val logState: MutableStateFlow<String> = MutableStateFlow("")
    }
}