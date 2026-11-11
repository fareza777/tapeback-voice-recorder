package com.f7developer.tapeback

import android.app.Application
import com.f7developer.tapeback.core.Notifications
import com.f7developer.tapeback.di.AppContainer

class TapeBackApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.start()
    }
}
