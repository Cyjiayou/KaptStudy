package com.cy.kaptstudy

import android.app.Application
import com.cy.runtime.ActivityBuilder

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        ActivityBuilder.INSTANCE.init(this)
    }
}