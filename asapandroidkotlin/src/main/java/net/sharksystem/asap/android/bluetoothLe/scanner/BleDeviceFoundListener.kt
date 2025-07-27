package net.sharksystem.asap.android.bluetoothLe.scanner

import android.bluetooth.BluetoothDevice

/**
 * Listener for found BLE devices
 */
interface BleDeviceFoundListener {
    /**
     * Called when a new BLE device is found
     */
    suspend fun onDeviceFound(device: BluetoothDevice)
}