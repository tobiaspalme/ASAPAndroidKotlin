package net.sharksystem.asap.android.bluetoothLe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.android.MacLayerEngine
import net.sharksystem.asap.android.bluetoothLe.scanner.BleDeviceFoundHandler
import net.sharksystem.asap.android.bluetoothLe.scanner.BleScanner
import net.sharksystem.asap.android.util.getFormattedTimestamp
import net.sharksystem.asap.android.util.getLogStart
import net.sharksystem.asap.android.util.hasRequiredBluetoothPermissions
import java.util.UUID

/**
 * The BleEngine manages Bluetooth Low Energy (BLE) communication.
 *
 * This class handles the setup, starting, and stopping of BLE scanning,
 * GATT server and GATT client functionalities.
 * It uses the [ASAPEncounterManager] to handle connections.
 * The default parameters are used for testing purposes.
 *
 * @param context Application context.
 * @param asapEncounterManager EncounterManager for handling ASAP encounters.
 * @param serviceUUID UUID of the BLE service to advertise and scan for. Defaults to [MacLayerEngine.DEFAULT_SERVICE_UUID].
 * @param characteristicUUID UUID of the BLE characteristic to use for communication. Defaults to [MacLayerEngine.DEFAULT_CHARACTERISTIC_UUID].
 */
@SuppressLint("MissingPermission")
class BleEngine(
    private val context: Context,
    private val asapEncounterManager: ASAPEncounterManager,
    private val serviceUUID: UUID = MacLayerEngine.DEFAULT_SERVICE_UUID,
    private val characteristicUUID: UUID = MacLayerEngine.DEFAULT_CHARACTERISTIC_UUID,
    private val waitBeforeReconnect: Long = MacLayerEngine.DEFAULT_WAIT_BEFORE_RECONNECT_TIME,
    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter,
    private val bleSocketHandler: BleSocketHandler = BleSocketHandler(asapEncounterManager),
    private val bleDeviceFoundHandler: BleDeviceFoundHandler = BleDeviceFoundHandler(
        context,
        serviceUUID,
        characteristicUUID,
        bleSocketHandler,
        waitBeforeReconnect
    ),
    private val bleScanner: BleScanner = BleScanner(
        bluetoothAdapter!!,
        serviceUUID,
        bleDeviceFoundHandler
    ),
    private val bleGattServer: BleGattServer = BleGattServer(
        context,
        bleSocketHandler,
        serviceUUID,
        characteristicUUID
    )
) : MacLayerEngine {

    private var isRunning: Boolean = false

    override fun start() {
        Log.d(this.getLogStart(), "Starting BleEngine")
        if (areStartRequirementsMet()) {
            setup()
            isRunning = true
            logState.value += "[${getFormattedTimestamp()}] BleEngine started\n"
        }
    }

    override fun stop() {
        if (isRunning) {
            Log.d(this.getLogStart(), "Stopping BleEngine")
            shutdown()
            isRunning = false
            logState.value += "[${getFormattedTimestamp()}] BleEngine stopped\n"
        } else {
            Log.w(this.getLogStart(), "BleEngine is not running")
        }
    }

    private fun setup() {
        startGattServer()
        startScanner()
    }

    private fun shutdown() {
        bleScanner.stopScan()
        stopGattServer()
        bleDeviceFoundHandler.stop()
    }

    private fun areStartRequirementsMet(): Boolean {
        if (isRunning) {
            Log.w(this.getLogStart(), "BleEngine is already running")
            return false
        }

        if (bluetoothAdapter == null) {
            Log.e(this.getLogStart(), "Device does not support bluetooth")
            logState.value += "[${getFormattedTimestamp()}] Device does not support bluetooth\n"
            return false
        }
        if (bluetoothAdapter.isEnabled.not()) {
            Log.e(this.getLogStart(), "Bluetooth is currently disabled")
            logState.value += "[${getFormattedTimestamp()}] Bluetooth is currently disabled\n"
            return false
        }

        if (context.hasRequiredBluetoothPermissions().not()) {
            Log.e(this.getLogStart(), "Bluetooth permissions not granted")
            logState.value += "[${getFormattedTimestamp()}] Bluetooth permissions not granted\n"
            return false
        }
        return true
    }

    private fun startGattServer() {
        bleGattServer.start()
    }

    private fun stopGattServer() {
        bleGattServer.stop()
    }

    private fun startScanner() {
        bleScanner.startScan()
    }

    @VisibleForTesting
    fun getIsRunning() = isRunning

    companion object {

        /**
         * only used for demonstration purpose (remove in future)
         */
        val logState: MutableStateFlow<String> = MutableStateFlow("")
    }
}