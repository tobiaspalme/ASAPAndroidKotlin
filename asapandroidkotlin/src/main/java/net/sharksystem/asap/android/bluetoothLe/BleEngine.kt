package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getLogStart
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class BleEngine(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
) : BleDeviceFoundListener {

    private lateinit var bleScanner: BleScanner
    private val openConnections: MutableMap<String, BleGattClient> = mutableMapOf()

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
        }
        startGattServer()
        startScanner()
    }

    private fun shutdown() {
        bleScanner.unregisterBleDeviceFoundListener(this)
        bleScanner.stopScan()
    }

    private fun startGattServer() {
        BleGattServerService.setASAPEncounterManager(asapEncounterManager)
        val intent = Intent(context, BleGattServerService::class.java).apply {
            action = BleGattServerService.ACTION_START_ADVERTISING
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun startScanner() {
        bleScanner = BleScanner(bluetoothAdapter!!)
        bleScanner.registerBleDeviceFoundListener(this)
        bleScanner.startScan()
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        val isConnectionAlreadyEstablished = isConnectionAlreadyEstablished(device.address)
        val shouldCreateConnectionToPeer = asapEncounterManager.shouldCreateConnectionToPeer(
            device.address,
            ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK
        )

        if (isConnectionAlreadyEstablished.not() && shouldCreateConnectionToPeer) {
            val bleGattClient = BleGattClient(context, device, asapEncounterManager)
            openConnections[device.address] = bleGattClient
            logState.value += "[${getTimestamp()}] Device: ${device.address} isConnected: true\n"
        }
    }

    private fun isConnectionAlreadyEstablished(macAddress: String): Boolean {
        val bleGattConnector = openConnections[macAddress]
        if (bleGattConnector != null) {
            if (bleGattConnector.isConnected) {
                Log.d(this.getLogStart(), "Connection already established to $macAddress")
                return true
            } else {
                Log.d(this.getLogStart(), "Connection not established anymore to $macAddress")
                openConnections.remove(macAddress)
                logState.value += "[${getTimestamp()}] Device: ${macAddress} isConnected: false\n"
                return false
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

    companion object {
        val logState: MutableStateFlow<String> = MutableStateFlow("")
    }
}