package com.jdoneill.weathermap

import android.app.Application
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher

import timber.log.Timber
import timber.log.Timber.DebugTree


class WeatherMap : Application() {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    override fun onCreate() {
        super.onCreate()
        setupLeakCanary()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    private fun setupLeakCanary(): RefWatcher {
        if (LeakCanary.isInAnalyzerProcess(this)) return RefWatcher.DISABLED
        return LeakCanary.install(this)
    }
}
