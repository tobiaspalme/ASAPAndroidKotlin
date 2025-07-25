package net.sharksystem.asap.android

import java.util.UUID

/**
 * Interface for interacting with different layer 2 protocols
 */
interface MacLayerEngine {

    /**
     * Call this function to start the layer 2 engine.
     * The process of discovery, connection and data transfer will start automatically.
     * Remember to check if the runtime permissions are granted before calling this function.
     */
    fun start()

    /**
     * Call this function to stop the layer 2 engine.
     * This function stops the discovery process and disconnects from every device.
     */
    fun stop()

    companion object {
        val DEFAULT_SERVICE_UUID: UUID = UUID.fromString("00002657-0000-1000-8000-00805f9b34fb")
        val DEFAULT_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("00004923-0000-1000-8000-00805f9b34fb")
        const val DEFAULT_WAIT_BEFORE_RECONNECT_TIME: Long = 5000
    }
}