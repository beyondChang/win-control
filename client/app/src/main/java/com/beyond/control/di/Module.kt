package com.beyond.control.di

import com.beyond.control.data.repository.DeviceRepository
import com.beyond.control.network.*
import com.beyond.control.ui.viewmodel.ControlViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DeviceRepository(androidContext()) }
    single<WebSocketConnection> { WebSocketConnectionImpl() }
    single { ConnectionManager(get<WebSocketConnection>(), get<DeviceRepository>()) }
    viewModel { ControlViewModel(get<DeviceRepository>(), get<ConnectionManager>()) }
}
