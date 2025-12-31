package com.example.ytdlpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ytdlpapp.data.repository.DownloadHistoryRepository
import com.example.ytdlpapp.data.repository.DownloadQueueRepository
import com.example.ytdlpapp.data.repository.ProxyRepository
import com.example.ytdlpapp.data.repository.SettingsRepository
import com.example.ytdlpapp.data.repository.StatisticsRepository
import com.example.ytdlpapp.domain.usecase.BinaryManager
import com.example.ytdlpapp.domain.usecase.DownloadEngine
import com.example.ytdlpapp.domain.usecase.QueueManager
import com.example.ytdlpapp.ui.navigation.Screen
import com.example.ytdlpapp.ui.screen.BatchProcessingScreen
import com.example.ytdlpapp.ui.screen.BinaryManagerScreen
import com.example.ytdlpapp.ui.screen.MainScreen
import com.example.ytdlpapp.ui.screen.ProxySettingsScreen
import com.example.ytdlpapp.ui.screen.SettingsScreen
import com.example.ytdlpapp.ui.screen.StatisticsScreen
import com.example.ytdlpapp.ui.theme.YtDlpAppTheme
import com.example.ytdlpapp.ui.viewmodel.BinaryManagerViewModel
import com.example.ytdlpapp.ui.viewmodel.MainViewModel
import com.example.ytdlpapp.ui.viewmodel.ProxySettingsViewModel
import com.example.ytdlpapp.ui.viewmodel.QueueManagerViewModel
import com.example.ytdlpapp.ui.viewmodel.SettingsViewModel
import com.example.ytdlpapp.ui.viewmodel.StatisticsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YtDlpAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 依存関係の初期化
                    val binaryManager = remember { BinaryManager(this@MainActivity) }
                    val settingsRepository = remember { SettingsRepository(this@MainActivity) }
                    val historyRepository = remember { DownloadHistoryRepository(this@MainActivity) }
                    val queueRepository = remember { DownloadQueueRepository(this@MainActivity) }
                    val proxyRepository = remember { ProxyRepository(this@MainActivity) }
                    val statisticsRepository = remember { StatisticsRepository(this@MainActivity) }
                    
                    val downloadEngine = remember {
                        DownloadEngine(
                            this@MainActivity,
                            binaryManager,
                            historyRepository
                        )
                    }
                    
                    val coroutineScope = remember {
                        CoroutineScope(Dispatchers.IO + SupervisorJob())
                    }
                    
                    val queueManager = remember {
                        QueueManager(
                            this@MainActivity,
                            queueRepository,
                            proxyRepository,
                            statisticsRepository,
                            downloadEngine,
                            coroutineScope
                        )
                    }

                    // ViewModels
                    val mainViewModel = remember {
                        MainViewModel(downloadEngine, settingsRepository, historyRepository)
                    }
                    val binaryManagerViewModel = remember {
                        BinaryManagerViewModel(binaryManager)
                    }
                    val settingsViewModel = remember {
                        SettingsViewModel(settingsRepository)
                    }
                    val queueManagerViewModel = remember {
                        QueueManagerViewModel(queueManager, queueRepository)
                    }
                    val proxySettingsViewModel = remember {
                        ProxySettingsViewModel(proxyRepository)
                    }
                    val statisticsViewModel = remember {
                        StatisticsViewModel(statisticsRepository)
                    }

                    // Navigation
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Main.route
                    ) {
                        composable(Screen.Main.route) {
                            MainScreen(
                                viewModel = mainViewModel,
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onBinaryManagerClick = {
                                    navController.navigate(Screen.BinaryManager.route)
                                },
                                onBatchProcessingClick = {
                                    navController.navigate(Screen.BatchProcessing.route)
                                },
                                onStatisticsClick = {
                                    navController.navigate(Screen.Statistics.route)
                                }
                            )
                        }

                        composable(Screen.BinaryManager.route) {
                            BinaryManagerScreen(
                                viewModel = binaryManagerViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.BatchProcessing.route) {
                            BatchProcessingScreen(
                                viewModel = queueManagerViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.ProxySettings.route) {
                            ProxySettingsScreen(
                                viewModel = proxySettingsViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.Statistics.route) {
                            StatisticsScreen(
                                viewModel = statisticsViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
