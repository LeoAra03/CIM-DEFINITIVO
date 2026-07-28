package com.sistema.distribuido.network.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.sistema.distribuido.network.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    @Provides
    @Singleton
    fun provideCommunicationCoordinator(permissionManager: PermissionManager): CommunicationCoordinator {
        return CommunicationCoordinator(permissionManager = permissionManager)
    }

    @Provides
    @Singleton
    fun provideBluetoothSppManager(
        @ApplicationContext context: Context,
        coordinator: CommunicationCoordinator
    ): BluetoothSppManager {
        return BluetoothSppManager(
            context = context,
            onLog = { msg -> android.util.Log.d("BT_SPP", msg) },
            onDataReceived = { mac, data ->
                android.util.Log.d("BT_SPP", "RX from $mac: $data")
            }
        )
    }

    @Provides
    @Singleton
    fun provideBluetoothHardwareManager(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): BluetoothHardwareManager {
        return BluetoothHardwareManager(
            context = context,
            onLog = { msg -> android.util.Log.d("BT_HW", msg) },
            onDataReceived = null,
            permissionManager = permissionManager
        )
    }
}
