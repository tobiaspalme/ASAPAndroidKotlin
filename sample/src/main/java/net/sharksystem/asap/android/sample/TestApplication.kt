package net.sharksystem.asap.android.sample

import android.app.Application
import net.sharksystem.asap.android.sample.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class TestApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TestApplication)
            modules(AppModule().module)
        }
    }
}