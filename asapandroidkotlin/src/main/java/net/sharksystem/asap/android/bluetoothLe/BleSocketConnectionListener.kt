package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothSocket

/**
 * Listener for BLE socket connections
 */
interface BleSocketConnectionListener {
    /**
     * Called when a BLE socket connection is established
     *
     * @param bluetoothSocket Bluetooth socket connection between local and remote device
     * @param initiator Whether the connection was initiated by the local device
     */
    fun onSuccessfulConnection(bluetoothSocket: BluetoothSocket, initiator: Boolean)
}