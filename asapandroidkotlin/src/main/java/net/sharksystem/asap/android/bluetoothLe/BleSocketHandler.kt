package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.delay
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.util.getFormattedTimestamp
import net.sharksystem.utils.streams.StreamPairImpl

@SuppressLint("MissingPermission")
class BleSocketHandler(
    private val asapEncounterManager: ASAPEncounterManager
) : BleSocketConnectionListener {
    override fun onSuccessfulConnection(
        bluetoothSocket: BluetoothSocket,
        initiator: Boolean
    ) {
        handleBTSocket(bluetoothSocket, initiator)
    }

    private fun handleBTSocket(socket: BluetoothSocket, initiator: Boolean) {
        val remoteMacAddress = socket.remoteDevice.address

        val streamPair = StreamPairImpl.getStreamPairWithEndpointAddress(
            socket.inputStream, socket.outputStream, remoteMacAddress
        )

        try {
            asapEncounterManager.handleEncounter(
                streamPair, ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK, initiator
            )
            if (socket.isConnected) {
                BleEngine.logState.value += "[${getFormattedTimestamp()}] Socket created to [${socket.remoteDevice.name}] with initiator: $initiator\n"
            } else {
                BleEngine.logState.value += "[${getFormattedTimestamp()}] Socket already exits to [${socket.remoteDevice.name}]\n"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}