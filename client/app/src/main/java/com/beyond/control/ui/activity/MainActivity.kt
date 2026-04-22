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
import org.koin.android.ext.android.inject
import com.beyond.control.ui.viewmodel.HomeViewModel
import com.beyond.control.ui.viewmodel.MouseViewModel
import com.beyond.control.ui.viewmodel.TouchpadViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by inject()
    private val touchpadViewModel: TouchpadViewModel by inject()
    private val mouseViewModel: MouseViewModel by inject()

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
                    }
                }
            }
        }
    }
}
