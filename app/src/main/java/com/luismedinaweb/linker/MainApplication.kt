package com.luismedinaweb.linker

import android.app.Application

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PrefHelper.initialize(this)
    }
}