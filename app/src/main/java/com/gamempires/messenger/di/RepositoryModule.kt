package com.gamempires.messenger.di

import com.gamempires.messenger.data.ChatRepository
import com.gamempires.messenger.data.nextcloud.NextcloudTalkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        nextcloudTalkRepository: NextcloudTalkRepository
    ): ChatRepository
}
