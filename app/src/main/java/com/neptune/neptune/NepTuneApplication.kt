package com.neptune.neptune

import android.app.Application
import android.content.Context

class NepTuneApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    appContext = applicationContext
  }

  companion object {
    lateinit var appContext: Context
      set // Restrict setting from outside the class
  }
}
