package net.sharksystem.asap.android.sample

import androidx.lifecycle.ViewModel
import net.sharksystem.asap.android.MacLayerEngine
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class TestScreenViewModel(private val macLayerEngine: MacLayerEngine) : ViewModel() {

    fun start() {
        macLayerEngine.start()
    }

    fun stop() {
        macLayerEngine.stop()
    }
}