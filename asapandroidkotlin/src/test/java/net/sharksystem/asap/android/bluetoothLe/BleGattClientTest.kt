package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothDevice
import android.content.Context
import io.mockk.mockk
import io.mockk.verify
import net.sharksystem.asap.ASAPEncounterManager
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BleGattClientTest {

    private val mockContext: Context = mockk<Context>(relaxed = true)
    private val mockBluetoothDevice: BluetoothDevice = mockk<BluetoothDevice>(relaxed = true)
    private val mockkAsapEncounterManager: ASAPEncounterManager =
        mockk<ASAPEncounterManager>(relaxed = true)
    private val serviceUUID: UUID = UUID.fromString("00002657-0000-1000-8000-00805f9b34fb")
    private val characteristicUUID: UUID = UUID.fromString("00004923-0000-1000-8000-00805f9b34fb")


    private lateinit var bleGattClient: BleGattClient

    @Before
    fun setup() {
        bleGattClient = BleGattClient(
            mockContext,
            mockBluetoothDevice,
            mockkAsapEncounterManager,
            serviceUUID,
            characteristicUUID,
        )
    }

    @Test
    fun `can connect to device`() {
        verify(exactly = 1) {
            mockBluetoothDevice.connectGatt(mockContext, false, any())
        }
    }
}