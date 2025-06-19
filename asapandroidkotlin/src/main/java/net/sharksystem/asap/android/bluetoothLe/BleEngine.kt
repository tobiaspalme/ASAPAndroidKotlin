package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getLogStart

class BleEngine(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
) {
    private lateinit var bleScanner: BleScanner

    init {
        Log.d(this.getLogStart(), "Initializing BleEngine")
        if (bluetoothAdapter == null) {
            Log.e(this.getLogStart(), "Device does not support bluetooth ")
        }

        //bleScanner = BluetoothLeScanner(context, asapEncounterManager, bluetoothAdapter)

        /*BleGattServerService.setASAPEncounterManager(asapEncounterManager)
        val intent = Intent(context, BleGattServerService::class.java).apply {
            action = BleGattServerService.ACTION_START_ADVERTISING
        }
        ContextCompat.startForegroundService(context, intent)*/

    }

    fun start() {
        Log.d(this.getLogStart(), "Starting BleEngine")
        setup()
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

    private fun startGattServer() {
        BleGattServerService.setASAPEncounterManager(asapEncounterManager)
        val intent = Intent(context, BleGattServerService::class.java).apply {
            action = BleGattServerService.ACTION_START_ADVERTISING
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun startScanner() {
        bleScanner = BleScanner(context, asapEncounterManager, bluetoothAdapter!!)
    }
}