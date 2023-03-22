package com.jdoneill.weathermap

import android.app.Application

import timber.log.Timber
import timber.log.Timber.DebugTree


class WeatherMap : Application() {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

}
