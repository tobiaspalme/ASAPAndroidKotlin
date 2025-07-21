package net.sharksystem.asap.android.bluetoothLe.scanner

import android.bluetooth.BluetoothDevice
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.bluetoothLe.BleGattClient
import net.sharksystem.asap.android.bluetoothLe.BleSocketConnectionListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BleDeviceFoundHandlerTest {

    private val mockContext: Context = mockk(relaxed = true)
    private val mockAsapEncounterManager: ASAPEncounterManager = mockk(relaxed = true)
    private val mockBleSocketConnectionListener: BleSocketConnectionListener = mockk(relaxed = true)
    private val mockBluetoothDevice: BluetoothDevice = mockk(relaxed = true)
    private val mockBleGattClient: BleGattClient = mockk(relaxed = true)

    private val serviceUUID: UUID = UUID.randomUUID()
    private val characteristicUUID: UUID = UUID.randomUUID()

    private lateinit var bleDeviceFoundHandler: BleDeviceFoundHandler

    private val testMacAddress = "00:1A:2B:3C:4D:5E"

    @Before
    fun setUp() {
        every { mockBluetoothDevice.address } returns testMacAddress

        bleDeviceFoundHandler = BleDeviceFoundHandler(
            context = mockContext,
            asapEncounterManager = mockAsapEncounterManager,
            serviceUUID = serviceUUID,
            characteristicUUID = characteristicUUID,
            bleSocketConnectionListener = mockBleSocketConnectionListener
        )
    }

    @Test
    fun `can create new connection when peer should be connected`() = runTest {
        every { mockAsapEncounterManager.shouldCreateConnectionToPeer(any(), any()) } returns true

        bleDeviceFoundHandler.onDeviceFound(mockBluetoothDevice)

        assertEquals(1, bleDeviceFoundHandler.getOpenConnections().size)
        assertNotNull(bleDeviceFoundHandler.getOpenConnections()[testMacAddress])
    }

    @Test
    fun `can not create connection when peer should not be connected`() = runTest {
        every { mockAsapEncounterManager.shouldCreateConnectionToPeer(any(), any()) } returns false

        bleDeviceFoundHandler.onDeviceFound(mockBluetoothDevice)

        assertTrue(bleDeviceFoundHandler.getOpenConnections().isEmpty())
    }

    @Test
    fun `can not create new connection if already established`() = runTest {
        every { mockAsapEncounterManager.shouldCreateConnectionToPeer(any(), any()) } returns true

        bleDeviceFoundHandler.onDeviceFound(mockBluetoothDevice)

        assertEquals(1, bleDeviceFoundHandler.getOpenConnections().size)

        every { mockBleGattClient.isDisconnected } returns false

        bleDeviceFoundHandler.onDeviceFound(mockBluetoothDevice)

        assertEquals(1, bleDeviceFoundHandler.getOpenConnections().size)
    }

    @Test
    fun `can create new connection if previous one was disconnected`() = runTest {
        every { mockAsapEncounterManager.shouldCreateConnectionToPeer(any(), any()) } returns true

        bleDeviceFoundHandler.onDeviceFound(mockBluetoothDevice)

        every { mockBleGattClient.isDisconnected } returns true

        bleDeviceFoundHandler.onDeviceFound(mockBluetoothDevice)

        verify(exactly = 2) {
            mockAsapEncounterManager.shouldCreateConnectionToPeer(
                testMacAddress,
                ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK
            )
        }
        assertEquals(1, bleDeviceFoundHandler.getOpenConnections().size)
    }
}
