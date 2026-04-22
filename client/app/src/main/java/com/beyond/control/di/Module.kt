package com.beyond.control.di

import com.beyond.control.data.repository.DeviceRepository
import com.beyond.control.network.*
import com.beyond.control.ui.viewmodel.HomeViewModel
import com.beyond.control.ui.viewmodel.MouseViewModel
import com.beyond.control.ui.viewmodel.TouchpadViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DeviceRepository() }
    single<WebSocketConnection> { WebSocketConnectionImpl() }
    single { ConnectionManager(get<WebSocketConnection>(), get<DeviceRepository>()) }
    viewModel { HomeViewModel(get<DeviceRepository>(), get<ConnectionManager>()) }
    viewModel { TouchpadViewModel(get()) }
    viewModel { MouseViewModel(get()) }
}
