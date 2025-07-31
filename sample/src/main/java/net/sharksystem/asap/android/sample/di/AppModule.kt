package net.sharksystem.asap.android.sample.di

import android.content.Context
import net.sharksystem.asap.ASAP
import net.sharksystem.asap.ASAPConnectionHandler
import net.sharksystem.asap.ASAPEncounterManager
import net.sharksystem.asap.ASAPEncounterManagerImpl
import net.sharksystem.asap.android.MacLayerEngine
import net.sharksystem.asap.android.bluetoothLe.BleEngine
import net.sharksystem.asap.android.sample.asap.TestASAPConnectionHandler
import net.sharksystem.asap.android.sample.TestScreenViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan("net.sharksystem.asap.android.sample")
class AppModule {

    @Single
    @Named("peerId")
    fun providePeerId(): String = ASAP.createUniqueID()

    @Single
    fun provideASAPConnectionHandler(
        @Named("peerId") peerId: String
    ): ASAPConnectionHandler = TestASAPConnectionHandler(peerId)

    @Single
    fun provideASAPEncounterManager(
        asapConnectionHandler: ASAPConnectionHandler,
        @Named("peerId") peerId: String
    ): ASAPEncounterManager = ASAPEncounterManagerImpl(
        asapConnectionHandler,
        peerId,
        5000
    )

    @Single
    fun provideBleEngine(
        context: Context,
        asapEncounterManager: ASAPEncounterManager,
    ): MacLayerEngine = BleEngine(
        context = context,
        asapEncounterManager = asapEncounterManager,
    )

    @Single
    fun provideTestScreenViewModel(
        macLayerEngine: MacLayerEngine,
    ) = TestScreenViewModel(macLayerEngine)
}