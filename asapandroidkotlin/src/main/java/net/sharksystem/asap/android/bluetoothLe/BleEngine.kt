package net.sharksystem.asap.android.bluetoothLe

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getLogStart

class BleEngine(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
) {
    private lateinit var bleScanner: BluetoothLeScanner

    init {
        Log.d(this.getLogStart(), "Initializing BleEngine")

        bleScanner = BluetoothLeScanner(context,asapEncounterManager)

        BleGattServerService.setASAPEncounterManager(asapEncounterManager)
        val intent = Intent(context, BleGattServerService::class.java).apply {
            action = BleGattServerService.ACTION_START_ADVERTISING
        }
        ContextCompat.startForegroundService(context, intent)

    }
}