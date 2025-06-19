package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getLogStart


@SuppressLint("MissingPermission")
class BleScanner(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
    private val bluetoothAdapter: BluetoothAdapter,
    private val bleScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val filter = ScanFilter.Builder().setServiceUuid(
        ParcelUuid(BleGattServerService.SERVICE_UUID)
    ).build()

    private val gattConnectionsList: MutableList<BleGattConnector> = mutableListOf()

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(
                this.getLogStart(),
                "onScanResult: ${result.device} ${result.scanRecord?.serviceUuids}"
            )
            if (gattConnectionsList.isEmpty()) {
                gattConnectionsList.add(
                    BleGattConnector(
                        context,
                        result.device,
                        asapEncounterManager
                    )
                )
            }
        }
    }

    init {
        coroutineScope.launch {
            bleScanner.startScan(listOf(filter), scanSettings, leScanCallback)
        }
    }
}