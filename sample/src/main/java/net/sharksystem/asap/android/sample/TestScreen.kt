package net.sharksystem.asap.android.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.sharksystem.asap.ASAP
import net.sharksystem.asap.ASAPEncounterManagerImpl
import net.sharksystem.asap.android.bluetoothLe.BleEngine


@Composable
internal fun TestScreen() {
    val peerId = ASAP.createUniqueID()
    val testASAPConnectionHandler = TestASAPConnectionHandler(peerId)
    val context = LocalContext.current
    val bleEngine = BleEngine(
        context,
        ASAPEncounterManagerImpl(testASAPConnectionHandler, peerId)
    )
    val logs by BleEngine.logState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            bleEngine.start()
        }) {
            Text("START BleEngine")
        }
        Text(logs)
    }
}