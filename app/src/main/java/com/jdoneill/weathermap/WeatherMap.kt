package com.jdoneill.weathermap

import android.app.Application
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher


class WeatherMap: Application() {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    override fun onCreate() {
        super.onCreate()
        setupLeakCanary()
    }

    protected fun setupLeakCanary():RefWatcher {
        if (LeakCanary.isInAnalyzerProcess(this)) return RefWatcher.DISABLED
        return LeakCanary.install(this)
    }
}
