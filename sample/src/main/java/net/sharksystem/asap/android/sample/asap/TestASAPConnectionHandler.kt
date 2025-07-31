package net.sharksystem.asap.android.sample.asap

import net.sharksystem.asap.ASAPConnectionHandler
import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.protocol.ASAPConnection
import java.io.InputStream
import java.io.OutputStream

class TestASAPConnectionHandler(private val peerId: CharSequence) : ASAPConnectionHandler {

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override fun handleConnection(
        p0: InputStream?,
        p1: OutputStream?,
        p2: Boolean,
        p3: Boolean,
        p4: MutableSet<CharSequence>?,
        p5: MutableSet<CharSequence>?
    ): ASAPConnection {
        TODO("Not yet implemented")
    }

    override fun handleConnection(
        p0: InputStream?,
        p1: OutputStream?,
        p2: Boolean,
        p3: Boolean,
        p4: ASAPEncounterConnectionType?,
        p5: MutableSet<CharSequence>?,
        p6: MutableSet<CharSequence>?
    ): ASAPConnection {
        TODO("Not yet implemented")
    }

    override fun handleConnection(p0: InputStream?, p1: OutputStream?): ASAPConnection {
        TODO("Not yet implemented")
    }

    override fun handleConnection(
        p0: InputStream?,
        p1: OutputStream?,
        p2: ASAPEncounterConnectionType?
    ): ASAPConnection {
        inputStream = p0
        outputStream = p1
        return TestASAPConnection(peerId)
    }
}