package com.compresso.app

import android.app.Application
import com.compresso.app.service.NotificationHelper

class CompressoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
    }
}
