package net.sharksystem.asap.android.bluetoothLe

import android.bluetooth.BluetoothAdapter
import android.content.Context
import io.mockk.mockk
import net.sharksystem.asap.ASAPEncounterManager
import org.junit.Before
import org.junit.Test

class BleEngineTest {

    private val mockContext: Context = mockk<Context>(relaxed = true)
    private val mockASAPEncounterManager: ASAPEncounterManager = mockk<ASAPEncounterManager>(relaxed = true)
    private val mockBluetoothAdapter: BluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)

    private lateinit var bleEngine: BleEngine


    @Before
    fun setUp(){
        bleEngine = BleEngine(
            context = mockContext,
            asapEncounterManager = mockASAPEncounterManager,
            bluetoothAdapter = mockBluetoothAdapter
        )
    }

    @Test
    fun `can start bleEngine`(){
        bleEngine.start()
    }
}