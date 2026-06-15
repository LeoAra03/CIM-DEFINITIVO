package com.sistema.distribuido.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothNetworkModule {

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothHardwareManager(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): BluetoothHardwareManager {
        return BluetoothHardwareManager(
            context = context,
            onLog = {},
            onDataReceived = null,
            permissionManager = permissionManager
        )
    }
}
