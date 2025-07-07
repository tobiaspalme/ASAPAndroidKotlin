package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothSocket

interface BleSocketConnectionListener {
    fun onSuccessfulConnection(bluetoothSocket: BluetoothSocket, initiator: Boolean)
}