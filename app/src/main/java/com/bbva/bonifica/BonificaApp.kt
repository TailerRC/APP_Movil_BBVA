package com.bbva.bonifica

import android.app.Application
import com.bbva.bonifica.di.AppContainer

class BonificaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
