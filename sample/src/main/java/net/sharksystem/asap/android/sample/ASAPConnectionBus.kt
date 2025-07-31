package net.sharksystem.asap.android.sample

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.InputStream
import java.io.OutputStream

object ASAPConnectionBus {
    private val _events = MutableSharedFlow<Pair<InputStream, OutputStream>>(replay = 1)
    val events = _events.asSharedFlow()

    fun publish(location: Pair<InputStream, OutputStream>) {
        _events.tryEmit(location)
    }

    suspend inline fun subscribe(crossinline onResult: (Pair<InputStream, OutputStream>) -> Unit) {
        events
            .collectLatest {
                onResult(it)
            }
    }
}