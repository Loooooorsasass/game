package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.RewardAdDialog
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.DailyChallengeScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ReplayScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.toastMessage) {
                    val msg = uiState.toastMessage
                    if (msg != null) {
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearToast()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    // Smooth animated screen transitions
                    AnimatedContent(
                        targetState = uiState.currentScreen,
                        transitionSpec = {
                            if (targetState == AppScreen.PLAYING || targetState == AppScreen.REPLAY) {
                                (slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(tween(350)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeOut(tween(300)))
                            } else {
                                (fadeIn(animationSpec = tween(300)) + slideInHorizontally(tween(300)) { -it / 4 })
                                    .togetherWith(fadeOut(animationSpec = tween(250)))
                            }
                        },
                        label = "ScreenTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) { targetScreen ->
                        when (targetScreen) {
                            AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                            AppScreen.PLAYING -> GameScreen(viewModel = viewModel)
                            AppScreen.REPLAY -> ReplayScreen(viewModel = viewModel)
                            AppScreen.LEADERBOARD -> LeaderboardScreen(viewModel = viewModel)
                            AppScreen.ACHIEVEMENTS -> AchievementsScreen(viewModel = viewModel)
                            AppScreen.DAILY_CHALLENGE -> DailyChallengeScreen(viewModel = viewModel)
                            AppScreen.SHOP -> ShopScreen(viewModel = viewModel)
                        }
                    }

                    // Reward Ad Dialog
                    val rewardPrompt = uiState.rewardAdPrompt
                    if (rewardPrompt != null) {
                        val title = when (rewardPrompt) {
                            "HINT" -> "Gợi ý đường đi chính xác"
                            "TIME" -> "+30 giây thời gian"
                            "COINS" -> "+50 Xu thưởng"
                            else -> "Phần thưởng"
                        }
                        RewardAdDialog(
                            rewardTitle = title,
                            onRewardEarned = { viewModel.onRewardEarned(rewardPrompt) },
                            onDismiss = { viewModel.dismissRewardedAd() }
                        )
                    }

                    // Cloud Sync Dialog
                    if (uiState.showCloudSyncDialog) {
                        CloudSyncDialog(
                            lastSyncTime = uiState.progress.lastCloudSyncTime,
                            totalStars = uiState.progress.totalStars,
                            highestCleared = uiState.progress.highestCleared,
                            onSyncConfirmed = { viewModel.confirmCloudSync() },
                            onDismiss = { viewModel.closeCloudSync() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.soundManager.resumeBgm()
    }

    override fun onPause() {
        super.onPause()
        viewModel.soundManager.pauseBgm()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.soundManager.release()
    }
}
