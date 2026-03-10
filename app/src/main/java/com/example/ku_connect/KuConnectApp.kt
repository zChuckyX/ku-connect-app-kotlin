package com.example.ku_connect

import android.app.Application
import com.example.ku_connect.service.CloudinaryService

class KuConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CloudinaryService.init(this)
    }
}