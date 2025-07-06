package net.sharksystem.asap.android.bluetoothLe

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

class BleScannerTest {

    private val mockBluetoothAdapter: BluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
    private val bluetoothLeScanner: BluetoothLeScanner = mockk<BluetoothLeScanner>(relaxed = true)
    private val scanSettings: ScanSettings = mockk<ScanSettings>(relaxed = true)
    private val scanFilter: ScanFilter = mockk<ScanFilter>(relaxed = true)
    private val serviceUuid: UUID = UUID.fromString("00002657-0000-1000-8000-00805f9b34fb")
    private val coroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private lateinit var bleScanner: BleScanner

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        bleScanner = BleScanner(
            mockBluetoothAdapter,
            serviceUuid,
            bluetoothLeScanner,
            scanSettings,
            scanFilter,
            coroutineScope
        )
    }

    @Test
    fun `can start and stop scan`() {
        bleScanner.startScan()

        coVerify(exactly = 1) {
            bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings, any<ScanCallback>())
        }

        bleScanner.stopScan()

        coVerify(exactly = 1) {
            bluetoothLeScanner.stopScan(any<ScanCallback>())
        }
    }
}