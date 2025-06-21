package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothDevice

interface BleDeviceFoundListener {
    fun onDeviceFound(device: BluetoothDevice)
}