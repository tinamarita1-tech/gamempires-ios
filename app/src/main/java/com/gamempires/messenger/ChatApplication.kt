package com.gamempires.messenger

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class ChatApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        com.gamempires.messenger.calls.CallDiagnostics.install(this)
        pushNotifier.ensureChannels()
        pushRegistrar.start()
    }

    @Inject
    lateinit var pushRegistrar: com.gamempires.messenger.push.PushRegistrar

    @Inject
    lateinit var pushNotifier: com.gamempires.messenger.push.PushNotifier

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
