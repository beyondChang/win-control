package com.beyond.control.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.beyond.control.ui.theme.RemoteAppTheme
import com.beyond.control.ui.screen.HomeScreen
import com.beyond.control.ui.screen.MouseControlScreen
import com.beyond.control.ui.screen.TouchpadScreen
import com.beyond.control.ui.viewmodel.HomeViewModel
import com.beyond.control.ui.viewmodel.MouseViewModel
import com.beyond.control.ui.viewmodel.TouchpadViewModel
import com.beyond.control.ui.viewmodel.VolumeViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.beyond.control.ui.screen.VolumeControlScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModel()
    private val touchpadViewModel: TouchpadViewModel by viewModel()
    private val mouseViewModel: MouseViewModel by viewModel()
    private val volumeViewModel: VolumeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemoteAppTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigate = { screen ->
                                    navController.navigate(screen)
                                }
                            )
                        }
                        composable("touchpad") {
                            TouchpadScreen(
                                viewModel = touchpadViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("mouse") {
                            MouseControlScreen(
                                viewModel = mouseViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("volume") {
                            VolumeControlScreen(
                                viewModel = volumeViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
