package net.sharksystem.asap.android.bluetoothLe.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.bluetoothLe.BleEngine.Companion.logState
import net.sharksystem.asap.android.bluetoothLe.BleGattClient
import net.sharksystem.asap.android.bluetoothLe.BleSocketConnectionListener
import net.sharksystem.asap.android.util.getFormattedTimestamp
import net.sharksystem.asap.android.util.getLogStart
import java.util.UUID

@SuppressLint("MissingPermission")
class BleDeviceFoundHandler(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
    private val serviceUUID: UUID,
    private val characteristicUUID: UUID,
    private val bleSocketConnectionListener: BleSocketConnectionListener,
) : BleDeviceFoundListener {

    private val openConnections: MutableMap<String, BleGattClient> = mutableMapOf()

    private val mutex = Mutex()

    override suspend fun onDeviceFound(device: BluetoothDevice) {
        mutex.withLock {
            val isConnectionAlreadyEstablished = isConnectionAlreadyEstablished(device.address)
            val shouldCreateConnectionToPeer = asapEncounterManager.shouldCreateConnectionToPeer(
                device.address,
                ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK
            )

            if (isConnectionAlreadyEstablished.not() && shouldCreateConnectionToPeer) {
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
        val bleGattConnector = openConnections[macAddress]
        if (bleGattConnector != null) {
            if (bleGattConnector.isDisconnected) {
                Log.d(this.getLogStart(), "Connection not established anymore to $macAddress")
                openConnections.remove(macAddress)
                return false
            } else {
                Log.d(this.getLogStart(), "Connection already established to $macAddress")
                return true
            }
        } else {
            return false
        }
    }

    fun stop(){
        openConnections.values.forEach { client ->
            client.closeGatt()
        }
        openConnections.clear()
    }
}