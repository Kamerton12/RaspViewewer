package com.example.raspviewerwidget

import android.app.Application
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig



class Aplicat: Application(){
    override fun onCreate() {
        super.onCreate()
        val config = YandexMetricaConfig.newConfigBuilder("ada14951-4f64-4959-97e4-9c0b6e82d783").build()
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(applicationContext, config)
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(this)
    }
}