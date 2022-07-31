package com.gerwalex.batteryguard.main

import com.gerwalex.batteryguard.database.DB

class App : com.gerwalex.lib.main.App() {

    override fun onCreate() {
        super.onCreate()
        DB.createInstance(this)
    }
}