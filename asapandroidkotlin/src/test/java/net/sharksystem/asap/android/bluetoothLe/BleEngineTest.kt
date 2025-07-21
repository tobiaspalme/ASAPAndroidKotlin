package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothAdapter
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.bluetoothLe.scanner.BleDeviceFoundHandler
import net.sharksystem.asap.android.bluetoothLe.scanner.BleScanner
import net.sharksystem.asap.android.util.hasRequiredBluetoothPermissions
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BleEngineTest {

    private val mockContext: Context = mockk<Context>(relaxed = true)
    private val mockASAPEncounterManager: ASAPEncounterManager =
        mockk<ASAPEncounterManager>(relaxed = true)
    private val mockBluetoothAdapter: BluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
    private val mockBleSocketHandler: BleSocketHandler = mockk<BleSocketHandler>(relaxed = true)
    private val mockBleDeviceFoundHandler: BleDeviceFoundHandler = mockk<BleDeviceFoundHandler>(relaxed = true)
    private val mockBleScanner: BleScanner = mockk<BleScanner>(relaxed = true)

    private lateinit var bleEngine: BleEngine

    @Before
    fun setUp() {
        mockkStatic(Context::hasRequiredBluetoothPermissions)

        bleEngine = BleEngine(
            context = mockContext,
            asapEncounterManager = mockASAPEncounterManager,
            bluetoothAdapter = mockBluetoothAdapter,
            bleSocketHandler = mockBleSocketHandler,
            bleDeviceFoundHandler = mockBleDeviceFoundHandler,
            bleScanner = mockBleScanner
        )
    }

    @After
    fun teardown() {
        unmockkStatic(Context::hasRequiredBluetoothPermissions)
    }

    @Test
    fun `can successfully start BleEngine`() {
        every { mockBluetoothAdapter.isEnabled } returns true
        every { mockContext.hasRequiredBluetoothPermissions() } returns true

        bleEngine.start()

        assertTrue(bleEngine.getIsRunning())
    }

    @Test
    fun `can not start BleEngine when bluetooth disabled`() {
        every { mockBluetoothAdapter.isEnabled } returns false

        bleEngine.start()

        assertFalse(bleEngine.getIsRunning())
    }

    @Test
    fun `can not start BleEngine when bluetooth permissions not granted`() {
        every { mockBluetoothAdapter.isEnabled } returns true
        every { mockContext.hasRequiredBluetoothPermissions() } returns false

        bleEngine.start()

        assertFalse(bleEngine.getIsRunning())
    }

    @Test
    fun `can not start BleEngine when already running`() {
        every { mockBluetoothAdapter.isEnabled } returns true
        every { mockContext.hasRequiredBluetoothPermissions() } returns true

        bleEngine.start()

        bleEngine.start()

        verify(exactly = 1) {
            mockBleScanner.startScan()
        }
    }

    @Test
    fun `can successfully stop BleEngine`() {
        every { mockBluetoothAdapter.isEnabled} returns true
        every { mockContext.hasRequiredBluetoothPermissions() } returns true

        bleEngine.start()

        bleEngine.stop()

        assertFalse(bleEngine.getIsRunning())

        verify {
            mockBleScanner.stopScan()
            mockBleDeviceFoundHandler.stop()
        }
    }

    @Test
    fun `can not stop BleEngine when not running`() {
        every { mockBluetoothAdapter.isEnabled} returns true
        every { mockContext.hasRequiredBluetoothPermissions() } returns true

        bleEngine.stop()

        verify(exactly = 0) {
            mockBleScanner.stopScan()
            mockBleDeviceFoundHandler.stop()
        }
    }

}