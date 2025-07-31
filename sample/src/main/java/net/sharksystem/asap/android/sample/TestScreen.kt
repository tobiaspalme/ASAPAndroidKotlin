package net.sharksystem.asap.android.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import net.sharksystem.asap.ASAP
import net.sharksystem.asap.ASAPEncounterManagerImpl
import net.sharksystem.asap.android.MacLayerEngine
import net.sharksystem.asap.android.bluetoothLe.BleEngine
import org.koin.compose.koinInject


@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun TestScreen() {

    val viewModel = koinInject<TestScreenViewModel>()

    val logs by BleEngine.logState.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            listOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.start()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.stop()
    }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.start()
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        FlowRow(
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Button(onClick = {
                viewModel.start()
            }) {
                Text("START BleEngine")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                viewModel.stop()
            }) {
                Text("STOP BleEngine")
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(top = 72.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(logs)
        }
    }
}