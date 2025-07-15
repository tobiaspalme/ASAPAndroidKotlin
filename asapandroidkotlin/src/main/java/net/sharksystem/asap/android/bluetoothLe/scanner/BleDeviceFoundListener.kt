package net.sharksystem.asap.android.bluetoothLe.scanner

import android.bluetooth.BluetoothDevice

interface BleDeviceFoundListener {
    suspend fun onDeviceFound(device: BluetoothDevice)
}