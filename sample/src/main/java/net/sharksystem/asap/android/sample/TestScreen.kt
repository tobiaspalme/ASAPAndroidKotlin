package net.sharksystem.asap.android.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import net.sharksystem.asap.ASAP
import net.sharksystem.asap.ASAPEncounterManagerImpl
import net.sharksystem.asap.android.bluetoothLe.BleEngine


@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun TestScreen() {
    val peerId = ASAP.createUniqueID()
    val testASAPConnectionHandler = TestASAPConnectionHandler(peerId)
    val context = LocalContext.current
    val bleEngine = remember {
        BleEngine(
            context,
            ASAPEncounterManagerImpl(testASAPConnectionHandler, peerId)
        )
    }
    val logs by BleEngine.logState.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION ,
                )
        } else {
            listOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            bleEngine.start()
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row {
            Button(onClick = {
                bleEngine.start()
            }) {
                Text("START BleEngine")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                bleEngine.stop()
            }) {
                Text("STOP BleEngine")
            }
        }
        Text(logs)
    }
}