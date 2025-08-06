package net.sharksystem.asap.android.bluetoothLe


import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BleGattClientTest {

    private val mockContext: Context = mockk(relaxed = true)
    private val mockDevice: BluetoothDevice = mockk(relaxed = true)
    private val mockListener: BleSocketConnectionListener = mockk(relaxed = true)
    private val mockGatt: BluetoothGatt = mockk(relaxed = true)
    private val mockGattService: BluetoothGattService = mockk(relaxed = true)
    private val mockCharacteristic: BluetoothGattCharacteristic = mockk(relaxed = true)

    private val gattCallbackSlot = slot<BluetoothGattCallback>()

    private lateinit var bleGattClient: BleGattClient

    private val serviceUUID: UUID = UUID.randomUUID()
    private val characteristicUUID: UUID = UUID.randomUUID()

    @Before
    fun setUp() {
        every {
            mockDevice.connectGatt(any(), any(), capture(gattCallbackSlot))
        } returns mockGatt

        bleGattClient = BleGattClient(
            context = mockContext,
            device = mockDevice,
            serviceUUID = serviceUUID,
            characteristicUUID = characteristicUUID,
            bleSocketConnectionListener = mockListener,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
        )
    }

    @Test
    fun `can connect to device when created`() {
        verify { mockDevice.connectGatt(mockContext, false, gattCallbackSlot.captured) }
    }

    @Test
    fun `can discoverServices when connected`() {
        val callback = gattCallbackSlot.captured

        callback.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        verify { mockGatt.discoverServices() }
    }

    @Test
    fun `can set isDisconnected to true when disconnected`() {
        val callback = gattCallbackSlot.captured

        callback.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_DISCONNECTED
        )

        assertTrue(bleGattClient.isDisconnected)
    }

    @Test
    fun `can read characteristic when services are discovered`() {
        every { mockGatt.getService(serviceUUID) } returns mockGattService
        every { mockGattService.getCharacteristic(characteristicUUID) } returns mockCharacteristic
        val callback = gattCallbackSlot.captured

        callback.onServicesDiscovered(mockGatt, BluetoothGatt.GATT_SUCCESS)

        verify { mockGatt.readCharacteristic(mockCharacteristic) }
    }

    @Test
    fun `can create L2CAP socket when characteristic read is successful`() = runTest {
        val testPsmValue = 4179
        val psmBytes = ByteBuffer.allocate(4).putInt(testPsmValue).array()

        every { mockListener.onSuccessfulConnection(any(), any()) } just runs
        val callback = gattCallbackSlot.captured

        callback.onCharacteristicRead(
            mockGatt,
            mockCharacteristic,
            psmBytes,
            BluetoothGatt.GATT_SUCCESS
        )

        coVerify { mockDevice.createInsecureL2capChannel(testPsmValue) }
        coVerify { mockListener.onSuccessfulConnection(any(), true) }
    }

    @Test
    fun `can close gatt connection`() = runTest {
        val callback = gattCallbackSlot.captured
        callback.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        bleGattClient.closeGatt()

        coVerify { mockGatt.disconnect() }
    }
}