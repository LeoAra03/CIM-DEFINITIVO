package com.sistema.distribuido.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommunicationCoordinatorModule {

    @Provides
    @Singleton
    fun provideCommunicationCoordinator(): CommunicationCoordinator {
        return CommunicationCoordinator()
    }
}
