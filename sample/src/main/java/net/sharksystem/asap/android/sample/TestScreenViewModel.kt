package net.sharksystem.asap.android.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.sharksystem.asap.android.MacLayerEngine
import net.sharksystem.asap.android.bluetoothLe.BleEngine
import net.sharksystem.asap.android.sample.asap.ASAPConnectionBus
import org.koin.android.annotation.KoinViewModel
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

@KoinViewModel
class TestScreenViewModel(private val macLayerEngine: MacLayerEngine) : ViewModel() {

    private val _uiState = MutableStateFlow(TestScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ASAPConnectionBus.subscribe { connection ->
                _uiState.update { currentState ->
                    currentState.copy(asapConnections = currentState.asapConnections + connection)
                }
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = connection.first
                        val buffer = ByteArray(1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            val receivedData = "Received Data: " + String(
                                buffer,
                                0,
                                bytesRead,
                                StandardCharsets.UTF_8
                            )
                            _uiState.update { currentState ->
                                currentState.copy(
                                    receivedDataList = currentState.receivedDataList + receivedData
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        viewModelScope.launch {
            BleEngine.logState.collect { logMessages ->
                _uiState.update { currentState ->
                    currentState.copy(logs = logMessages)
                }
            }
        }
    }

    fun start() {
        macLayerEngine.start()
    }

    fun stop() {
        macLayerEngine.stop()
        _uiState.value.asapConnections.forEach { (inputStream, outputStream) ->
            try {
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessageToAllConnectedDevices(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.asapConnections.forEach { (_, outputStream) ->
                try {
                    outputStream.write(message.toByteArray(StandardCharsets.UTF_8))
                    outputStream.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

data class TestScreenUiState(
    val asapConnections: List<Pair<InputStream, OutputStream>> = emptyList(),
    val receivedDataList: List<String> = emptyList(),
    val logs: String = "",
)