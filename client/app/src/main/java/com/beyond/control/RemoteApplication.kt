package com.beyond.control

import android.app.Application
import com.beyond.control.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RemoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@RemoteApplication)
            modules(appModule)
        }
    }
}
