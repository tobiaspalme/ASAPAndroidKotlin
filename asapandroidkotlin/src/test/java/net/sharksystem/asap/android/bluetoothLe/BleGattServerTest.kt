package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BleGattServerTest {

    private val mockContext: Context = mockk(relaxed = true)
    private val mockListener: BleSocketConnectionListener = mockk(relaxed = true)
    private val mockBluetoothManager: BluetoothManager = mockk(relaxed = true)
    private val mockAdapter: BluetoothAdapter = mockk(relaxed = true)
    private val mockAdvertiser: BluetoothLeAdvertiser = mockk(relaxed = true)
    private val mockGattServer: BluetoothGattServer = mockk(relaxed = true)
    private val mockServerSocket: BluetoothServerSocket = mockk(relaxed = true)
    private val coroutineScope = CoroutineScope(StandardTestDispatcher())

    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()

    private lateinit var bleGattServer: BleGattServer

    private val gattServerCallbackSlot = slot<BluetoothGattServerCallback>()

    @Before
    fun setUp() {
        mockkConstructor(AdvertiseSettings.Builder::class)
        every { anyConstructed<AdvertiseSettings.Builder>().setAdvertiseMode(any()) } returns
                mockk(relaxed = true)
        every { anyConstructed<AdvertiseSettings.Builder>().setTxPowerLevel(any()) } returns
                mockk(relaxed = true)
        every { anyConstructed<AdvertiseSettings.Builder>().setConnectable(any()) } returns
                mockk(relaxed = true)
        every { anyConstructed<AdvertiseSettings.Builder>().setTimeout(any()) } returns
                mockk(relaxed = true)

        mockkConstructor(AdvertiseData.Builder::class)
        every { anyConstructed<AdvertiseData.Builder>().setIncludeDeviceName(any()) } returns
                mockk(relaxed = true)
        every { anyConstructed<AdvertiseData.Builder>().addServiceUuid(any()) } returns
                mockk(relaxed = true)


        every { mockBluetoothManager.adapter } returns mockAdapter
        every { mockAdapter.bluetoothLeAdvertiser } returns mockAdvertiser
        every {
            mockBluetoothManager.openGattServer(
                any(),
                capture(gattServerCallbackSlot)
            )
        } returns mockGattServer
        every { mockAdapter.listenUsingInsecureL2capChannel() } returns mockServerSocket

        bleGattServer = BleGattServer(
            context = mockContext,
            bleSocketConnectionListener = mockListener,
            serviceUUID = serviceUUID,
            characteristicUUID = characteristicUUID,
            bluetoothManager = mockBluetoothManager,
            adapter = mockAdapter,
            advertiser = mockAdvertiser,
            scope = coroutineScope
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `can initialize server, starts advertising and listens for connections`() = runTest {
        bleGattServer.start()

        verify { mockBluetoothManager.openGattServer(mockContext, any()) }
        verify { mockGattServer.addService(any()) }
        verify { mockAdvertiser.startAdvertising(any(), any(), any()) }
        verify { mockAdapter.listenUsingInsecureL2capChannel() }

        bleGattServer.stop()
    }

    @Test
    fun `can stop advertising and cancel connections`() = runTest {
        val mockDevice = mockk<BluetoothDevice>(relaxed = true)
        bleGattServer.start()
        gattServerCallbackSlot.captured.onConnectionStateChange(
            mockDevice,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothGatt.STATE_CONNECTED
        )

        bleGattServer.stop()

        verify { mockAdvertiser.stopAdvertising(any<AdvertiseCallback>()) }
        verify { mockGattServer.cancelConnection(mockDevice) }
        verify { mockGattServer.close() }
    }
}