package com.gamempires.messenger.di

import com.gamempires.messenger.BuildConfig
import com.gamempires.messenger.data.nextcloud.api.NextcloudTalkApi
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import com.gamempires.messenger.data.nextcloud.dto.NextcloudMessageDto
import com.gamempires.messenger.data.nextcloud.dto.NextcloudMessageDtoAdapter
import com.gamempires.messenger.data.nextcloud.dto.NextcloudRoomDto
import com.gamempires.messenger.data.nextcloud.dto.NextcloudRoomDtoAdapter
import com.gamempires.messenger.data.nextcloud.interceptor.NextcloudAuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NextcloudNetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        val messageAdapter = NextcloudMessageDtoAdapter()
        val roomAdapter = NextcloudRoomDtoAdapter(messageAdapter)
        return Moshi.Builder()
            .add(NextcloudMessageDto::class.java, messageAdapter)
            .add(NextcloudRoomDto::class.java, roomAdapter)
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: NextcloudAuthInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Request logging only in debug builds; release builds log nothing to logcat.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS) // Long polling requires >30s read timeout
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NextcloudAuthStorage.DEFAULT_SERVER_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideNextcloudTalkApi(
        retrofit: Retrofit
    ): NextcloudTalkApi {
        return retrofit.create(NextcloudTalkApi::class.java)
    }
}
