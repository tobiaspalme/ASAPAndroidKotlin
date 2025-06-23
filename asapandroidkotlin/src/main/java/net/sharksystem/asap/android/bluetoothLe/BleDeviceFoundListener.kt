package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothDevice

interface BleDeviceFoundListener {
    suspend fun onDeviceFound(device: BluetoothDevice)
}