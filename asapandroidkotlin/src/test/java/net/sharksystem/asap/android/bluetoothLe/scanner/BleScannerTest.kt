package net.sharksystem.asap.android.bluetoothLe.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BleScannerTest {

    private val mockBluetoothAdapter: BluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
    private val mockBleDeviceFoundListener: BleDeviceFoundListener =
        mockk<BleDeviceFoundListener>(relaxed = true)
    private val mockBluetoothLeScanner: BluetoothLeScanner =
        mockk<BluetoothLeScanner>(relaxed = true)
    private val mockScanSettings: ScanSettings = mockk<ScanSettings>(relaxed = true)
    private val mockScanFilter: ScanFilter = mockk<ScanFilter>(relaxed = true)
    private val mockServiceUuid: UUID = UUID.fromString("00002657-0000-1000-8000-00805f9b34fb")
    private val mockCoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private lateinit var bleScanner: BleScanner

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        bleScanner = BleScanner(
            bluetoothAdapter = mockBluetoothAdapter,
            serviceUUID = mockServiceUuid,
            bleDeviceFoundListener = mockBleDeviceFoundListener,
            bluetoothLeScanner = mockBluetoothLeScanner,
            scanSettings = mockScanSettings,
            filter = mockScanFilter,
            coroutineScope = mockCoroutineScope
        )
    }

    @Test
    fun `can start scan`() {
        bleScanner.startScan()

        coVerify(exactly = 1) {
            mockBluetoothLeScanner.startScan(
                listOf(mockScanFilter),
                mockScanSettings,
                any<ScanCallback>()
            )
        }
    }

    @Test
    fun `can stop scan`() {
        bleScanner.startScan()

        bleScanner.stopScan()

        coVerify(exactly = 1) {
            mockBluetoothLeScanner.stopScan(any<ScanCallback>())
        }
    }
}