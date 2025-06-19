package net.sharksystem.asap.android.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import net.sharksystem.asap.ASAP
import net.sharksystem.asap.ASAPEncounterManagerImpl
import net.sharksystem.asap.android.bluetoothLe.BleEngine

@Composable
internal fun TestScreen() {
    val testASAPConnectionHandler = TestASAPConnectionHandler()
    val context = LocalContext.current
    val bleEngine = BleEngine(
        context,
        ASAPEncounterManagerImpl(testASAPConnectionHandler, ASAP.createUniqueID())
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("TEST")
    }
}