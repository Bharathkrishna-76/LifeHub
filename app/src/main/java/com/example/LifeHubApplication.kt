package com.example

import android.app.Application
import com.example.data.worker.ReminderScheduler
import com.example.di.AppContainer

class LifeHubApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.ensureChannel(this)
    }
}
