package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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


@SuppressLint("MissingPermission")
class BleScanner(
    private val bluetoothAdapter: BluetoothAdapter,
    private val bleScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner,
    private val scanSettings: ScanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(),
    private val filter: ScanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(BleGattServerService.SERVICE_UUID)).build(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val deviceFoundListeners: MutableList<BleDeviceFoundListener> = mutableListOf()

    fun startScan() {
        Log.d(this.getLogStart(), "Starting scan")
        coroutineScope.launch {
            bleScanner.startScan(listOf(filter), scanSettings, leScanCallback)
        }
    }

    fun stopScan() {
        Log.d(this.getLogStart(), "Stopping scan")
        bleScanner.stopScan(leScanCallback)
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(
                this.getLogStart(),
                "onScanResult: ${result.device} ${result.scanRecord?.serviceUuids}"
            )
            notifyBleDeviceFound(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(this.getLogStart(), "onScanFailed: $errorCode")
        }
    }

    private fun notifyBleDeviceFound(device: BluetoothDevice) {
        deviceFoundListeners.forEach {
            it.onDeviceFound(device)
        }
    }

    fun registerBleDeviceFoundListener(listener: BleDeviceFoundListener) {
        deviceFoundListeners.add(listener)
    }

    fun unregisterBleDeviceFoundListener(listener: BleDeviceFoundListener) {
        deviceFoundListeners.remove(listener)
    }
}