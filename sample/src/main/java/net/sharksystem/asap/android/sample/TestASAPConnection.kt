package net.sharksystem.asap.android.sample

import net.sharksystem.asap.ASAPEncounterConnectionType
import net.sharksystem.asap.protocol.ASAPConnection
import net.sharksystem.asap.protocol.ASAPConnectionListener
import net.sharksystem.asap.protocol.ASAPOnlineMessageSource

class TestASAPConnection(
    private val peerId: CharSequence
) : ASAPConnection {
    override fun getEncounteredPeer(): CharSequence? {
        return peerId
    }

    override fun addOnlineMessageSource(p0: ASAPOnlineMessageSource?) {

    }

    override fun removeOnlineMessageSource(p0: ASAPOnlineMessageSource?) {

    }

    override fun addASAPConnectionListener(p0: ASAPConnectionListener?) {

    }

    override fun removeASAPConnectionListener(p0: ASAPConnectionListener?) {

    }

    override fun isSigned(): Boolean {
        return false
    }

    override fun getASAPEncounterConnectionType(): ASAPEncounterConnectionType? {
        return ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK
    }

    override fun kill() {

    }
}