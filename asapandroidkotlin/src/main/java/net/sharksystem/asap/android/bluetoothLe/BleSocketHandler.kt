package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothSocket
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.utils.streams.StreamPairImpl

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

        asapEncounterManager.handleEncounter(
            streamPair, ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK, initiator
        )
    }
}