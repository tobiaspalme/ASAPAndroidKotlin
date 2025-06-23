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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getLogStart
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("MissingPermission")
class BleEngine(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
) : BleDeviceFoundListener {

    private var bleGattServerService: BleGattServerService? = null
    private val bleScanner: BleScanner = BleScanner(bluetoothAdapter!!)
    private val openConnections: MutableMap<String, BleGattClient> = mutableMapOf()
    private val mutex = Mutex()

    fun start() {
        Log.d(this.getLogStart(), "Starting BleEngine")
        setup()
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
        bleScanner.unregisterBleDeviceFoundListener(this)
        bleScanner.stopScan()
        stopGattServer()
        openConnections.clear()
    }

    private fun startGattServer() {
        BleGattServerService.setASAPEncounterManager(asapEncounterManager)

        val intent = Intent(context, BleGattServerService::class.java)
        context.bindService(intent,bleGattServerConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopGattServer() {
        context.unbindService(bleGattServerConnection)
    }

    private fun startScanner() {
        bleScanner.registerBleDeviceFoundListener(this)
        bleScanner.startScan()
    }

    override suspend fun onDeviceFound(device: BluetoothDevice) {
        mutex.withLock {
            val isConnectionAlreadyEstablished = isConnectionAlreadyEstablished(device.address)
            val shouldCreateConnectionToPeer = asapEncounterManager.shouldCreateConnectionToPeer(
                device.address,
                ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK
            )

            if (isConnectionAlreadyEstablished.not() && shouldCreateConnectionToPeer) {
                val bleGattClient = BleGattClient(context, device, asapEncounterManager)
                openConnections[device.address] = bleGattClient
                logState.value += "[${getTimestamp()}] Device: ${device.name} ${device.address} isConnected: true\n"
            }
        }
    }

    private fun isConnectionAlreadyEstablished(macAddress: String): Boolean {
        val bleGattConnector = openConnections[macAddress]
        if (bleGattConnector != null) {
            if (bleGattConnector.isDisconnected) {
                Log.d(this.getLogStart(), "Connection not established anymore to $macAddress")
                openConnections.remove(macAddress)
                logState.value += "[${getTimestamp()}] Device: ${macAddress} isConnected: false\n"
                return false
            } else {
                Log.d(this.getLogStart(), "Connection already established to $macAddress")
                return true
            }
        } else {
            return false
        }
    }

    private fun getTimestamp(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return now.format(formatter)
    }

    private val bleGattServerConnection = object : ServiceConnection{
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            Log.d(this.getLogStart(), "Service connected")
            val binder = service as BleGattServerService.BleGattServerBinder
            bleGattServerService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(this.getLogStart(), "Service disconnected")
        }

    }

    private fun checkPermissions() {

    }

    companion object {
        // only used for demonstration purpose
        val logState: MutableStateFlow<String> = MutableStateFlow("")
    }
}