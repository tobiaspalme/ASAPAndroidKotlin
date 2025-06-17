package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.bluetoothLe.BleGattConnector
import net.sharksystem.asap.android.bluetoothLe.BleGattServerService
import net.sharksystem.asap.android.util.getLogStart

@SuppressLint("MissingPermission")
class BluetoothLeScanner(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
) {
    private val handler = Handler(Looper.getMainLooper())

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val filter = ScanFilter.Builder().setServiceUuid(
        ParcelUuid(BleGattServerService.SERVICE_UUID)
    ).build()

    private val gattConnectionsList: MutableList<BleGattConnector> = mutableListOf()

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(
                this.getLogStart(),
                "onScanResult: ${result.device} ${result.scanRecord?.serviceUuids}"
            )
            if (gattConnectionsList.isEmpty()) {
                gattConnectionsList.add(BleGattConnector(context, result.device,asapEncounterManager))
            }
        }
    }

    init {
        handler.post {
            bleScanner.startScan(listOf(filter), scanSettings, leScanCallback)
        }
    }
}