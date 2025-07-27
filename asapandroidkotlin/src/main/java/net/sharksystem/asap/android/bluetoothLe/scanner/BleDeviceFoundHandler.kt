package net.sharksystem.asap.android.bluetoothLe.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.sharksystem.asap.android.bluetoothLe.BleGattClient
import net.sharksystem.asap.android.bluetoothLe.BleSocketConnectionListener
import net.sharksystem.asap.android.util.getLogStart
import java.util.Date
import java.util.UUID

@SuppressLint("MissingPermission")
class BleDeviceFoundHandler(
    private val context: Context,
    private val serviceUUID: UUID,
    private val characteristicUUID: UUID,
    private val bleSocketConnectionListener: BleSocketConnectionListener,
    private val waitBeforeReconnect: Long,
) : BleDeviceFoundListener {

    private val openConnections: MutableMap<String, BleGattClient> = mutableMapOf()

    private val encounteredDevices: MutableMap<String, Date> = mutableMapOf()

    private val mutex = Mutex()

    override suspend fun onDeviceFound(device: BluetoothDevice) {
        mutex.withLock {
            cleanUpConnections()
            val isConnectionAlreadyEstablished = isConnectionAlreadyEstablished(device.address)
            val shouldConnectToMACPeer = shouldConnectToMACPeer(device.address)

            Log.d(
                this.getLogStart(),
                "----> onDeviceFound: ${device.name} | isConnectionAlreadyEstablished: $isConnectionAlreadyEstablished | shouldConnectToMACPeer: $shouldConnectToMACPeer"
            )

            if (isConnectionAlreadyEstablished.not() && shouldConnectToMACPeer) {
                val bleGattClient = BleGattClient(
                    context,
                    device,
                    serviceUUID,
                    characteristicUUID,
                    bleSocketConnectionListener,
                )
                openConnections[device.address] = bleGattClient
            }
        }
    }

    private fun cleanUpConnections() {
        openConnections.values.forEach { client ->
            if (client.isDisconnected) {
                Log.d(this.getLogStart(), "Connection is disconnected to ${client.device.address}")
                client.closeGatt()
            }
            if (client.bluetoothSocket == null) {
                Log.d(this.getLogStart(), "bluetoothSocket is pending")
            }
            if (client.bluetoothSocket?.isConnected?.not() == true) {
                Log.d(
                    this.getLogStart(),
                    "Connection not established anymore to ${client.device.address}"
                )
                client.closeGatt()
            }
        }
        // This approach is used to avoid a potential ConcurrentModificationException
        openConnections.values.removeIf { it.isDisconnected || it.bluetoothSocket?.isConnected?.not() == true }
    }

    private fun isConnectionAlreadyEstablished(macAddress: String): Boolean {
        val client = openConnections[macAddress] ?: return false
        return client.isDisconnected.not()
    }

    private fun shouldConnectToMACPeer(macAddress: String): Boolean {
        val now = Date()
        val lastEncounter = encounteredDevices[macAddress]

        if (lastEncounter == null) {
            Log.d(this.getLogStart(), "device not in encounteredDevices")
            encounteredDevices.put(macAddress, now)
            return true
        }

        val nowInMillis = System.currentTimeMillis()
        val reconnectedBeforeInMillis: Long = nowInMillis - waitBeforeReconnect
        val reconnectBefore = Date(reconnectedBeforeInMillis)

        if (lastEncounter.before(reconnectBefore)) {
            Log.d(this.getLogStart(), "should connect - not recently met: $macAddress")
            encounteredDevices.put(macAddress, now)
            return true
        }

        Log.d(this.getLogStart(), "should not connect - recently met: $macAddress")
        return false
    }

    fun stop() {
        openConnections.values.forEach { client ->
            client.closeGatt()
        }
        openConnections.clear()
    }

    @VisibleForTesting
    fun getOpenConnections() = openConnections
}