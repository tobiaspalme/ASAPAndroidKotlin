package net.sharksystem.asap.android.bluetoothLe.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.bluetoothLe.BleGattClient
import net.sharksystem.asap.android.bluetoothLe.BleSocketConnectionListener
import net.sharksystem.asap.android.util.getLogStart
import java.util.Date
import java.util.UUID

@SuppressLint("MissingPermission")
class BleDeviceFoundHandler(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
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
            val isConnectionAlreadyEstablished = isConnectionAlreadyEstablished(device.address)
            val shouldConnectToMACPeer = shouldConnectToMACPeer(device.address)

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

    private fun isConnectionAlreadyEstablished(macAddress: String): Boolean {
        val bleGattClient = openConnections[macAddress]
        if (bleGattClient != null) {
            if (bleGattClient.isDisconnected) {
                Log.d(this.getLogStart(), "Connection already disconnected to $macAddress")
                bleGattClient.closeGatt()
                openConnections.remove(macAddress)
                return false
            }
            if (bleGattClient.bluetoothSocket == null) {
                Log.d(this.getLogStart(), "bluetoothSocket is pending")
                return true
            }
            if (bleGattClient.bluetoothSocket?.isConnected?.not() == true) {
                Log.d(this.getLogStart(), "Connection not established anymore to $macAddress")
                bleGattClient.closeGatt()
                openConnections.remove(macAddress)
                return false
            } else {
                Log.d(
                    this.getLogStart(),
                    "Connection already established to $macAddress"
                )
                return true
            }
        } else {
            return false
        }
    }

    private fun shouldConnectToMACPeer(macAddress: String): Boolean {
        val now = Date()
        val lastEncounter = this.encounteredDevices[macAddress]

        if (lastEncounter == null) {
            Log.d(this.getLogStart(), "device not in encounteredDevices - should connect")
            this.encounteredDevices.put(macAddress, now)
            return true
        }

        val nowInMillis = System.currentTimeMillis()
        val reconnectedBeforeInMillis: Long = nowInMillis - this.waitBeforeReconnect
        val reconnectBefore = Date(reconnectedBeforeInMillis)

        if (lastEncounter.before(reconnectBefore)) {
            Log.d(this.getLogStart(), "yes - should connect: $macAddress")
            this.encounteredDevices.put(macAddress, now)
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