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
import org.koin.android.annotation.KoinViewModel
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

@KoinViewModel
class TestScreenViewModel(private val macLayerEngine: MacLayerEngine) : ViewModel() {

    val logs = BleEngine.logState

    private val _uiState = MutableStateFlow(TestScreenUiState(emptyList()))
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
                            val receivedData = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                            _uiState.update { currentState ->
                                currentState.copy(receivedData = receivedData)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

    }

    fun start() {
        macLayerEngine.start()
    }

    fun stop() {
        macLayerEngine.stop()
    }

    fun sendHelloWorld() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.asapConnections.first().let { (_, outputStream) ->
                try {
                    outputStream.write("Hello World".toByteArray(StandardCharsets.UTF_8))
                    outputStream.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

data class TestScreenUiState(
    val asapConnections: List<Pair<InputStream, OutputStream>>,
    val receivedData: String? = null
)