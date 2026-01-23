package com.example.worthit

import android.app.Application

class WorthItApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}