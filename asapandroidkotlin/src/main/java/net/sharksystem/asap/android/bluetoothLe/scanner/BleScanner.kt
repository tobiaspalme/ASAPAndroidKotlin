package net.sharksystem.asap.android.bluetoothLe.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sharksystem.asap.android.util.getLogStart
import java.util.UUID

/**
 * Scans for nearby Bluetooth Low Energy (BLE) devices that advertise a specific service UUID
 */
@SuppressLint("MissingPermission")
class BleScanner(
    private val bluetoothAdapter: BluetoothAdapter,
    private val serviceUUID: UUID,
    private val bleDeviceFoundListener: BleDeviceFoundListener,
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner,
    private val scanSettings: ScanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(),
    private val filter: ScanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(serviceUUID)).build(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    /**
     * Starts scanning for BLE devices that advertise a specified service UUID
     */
    fun startScan() {
        Log.d(this.getLogStart(), "Starting scan")
        coroutineScope.launch {
            if (bluetoothLeScanner == null) {
                Log.e(this.getLogStart(), "BluetoothLeScanner is null")
            } else {
                bluetoothLeScanner.startScan(listOf(filter), scanSettings, leScanCallback)
            }
        }
    }

    /**
     * Stops scanning for BLE devices
     */
    fun stopScan() {
        Log.d(this.getLogStart(), "Stopping scan")
        coroutineScope.launch {
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(
                this.getLogStart(),
                "----> onScanResult: ${result.device} ${result.scanRecord?.serviceUuids}"
            )
            coroutineScope.launch {
                bleDeviceFoundListener.onDeviceFound(result.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(this.getLogStart(), "onScanFailed: $errorCode")
        }
    }
}