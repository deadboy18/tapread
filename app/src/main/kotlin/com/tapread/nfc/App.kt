package com.tapread.nfc

import android.app.Application
import org.slf4j.LoggerFactory

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        val log = LoggerFactory.getLogger("TapRead")
        log.info("TapRead v{} starting", BuildConfig.VERSION_NAME)
    }
}
