package com.arjunvision.arjunfitai.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arjunvision.arjunfitai.data.samsung.SamsungHealthRepository
import com.arjunvision.arjunfitai.data.settings.SettingsRepository
import com.arjunvision.arjunfitai.data.settings.UserSettings
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.connect.ConnectScreen
import com.arjunvision.arjunfitai.ui.components.LocalReduceMotion
import com.arjunvision.arjunfitai.ui.components.systemReducesMotion
import com.arjunvision.arjunfitai.ui.dashboard.DashboardScreen
import com.arjunvision.arjunfitai.ui.dashboard.DashboardUiState
import com.arjunvision.arjunfitai.ui.dashboard.DashboardViewModel
import com.arjunvision.arjunfitai.ui.detail.HeartRateScreen
import com.arjunvision.arjunfitai.ui.detail.SleepScreen
import com.arjunvision.arjunfitai.ui.detail.StepsScreen
import com.arjunvision.arjunfitai.ui.home.HomeActions
import com.arjunvision.arjunfitai.ui.home.HomeScreen
import com.arjunvision.arjunfitai.ui.placeholder.ComingSoonScreen
import com.arjunvision.arjunfitai.ui.profile.ProfileScreen
import com.arjunvision.arjunfitai.ui.splash.SplashScreen
import com.arjunvision.arjunfitai.ui.sync.HealthSyncScreen
import com.arjunvision.arjunfitai.ui.theme.AfColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private object Routes {
    const val HOME = "home"
    const val WORKOUT = "workout"
    const val NUTRITION = "nutrition"
    const val PROGRESS = "progress"
    const val STEPS = "steps"
    const val HEART_RATE = "heart-rate"
    const val SLEEP = "sleep"
    const val OXYGEN = "sleep-oxygen"
    const val WEIGHT = "weight"
    const val COACH = "coach"
    const val PROFILE = "profile"
    const val HEALTH_SYNC = "health-sync"
    const val CONNECT = "connect"
    const val ALL_DATA = "all-data"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab(Routes.HOME, "Home", AfIcons.Home),
    Tab(Routes.WORKOUT, "Workout", AfIcons.Workout),
    Tab(Routes.NUTRITION, "Nutrition", AfIcons.Nutrition),
    Tab(Routes.PROGRESS, "Progress", AfIcons.Progress),
)

@Composable
fun ArjunFitApp() {
    val appContext = LocalContext.current.applicationContext
    val repository = remember { SamsungHealthRepository(appContext) }
    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory { initializer { DashboardViewModel(repository) } }
    )
    val settingsRepository = remember { SettingsRepository(appContext) }
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val state by viewModel.state.collectAsStateWithLifecycle()

    var reduceMotion by remember { mutableStateOf(systemReducesMotion()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        reduceMotion = systemReducesMotion()
        // Coming back from Samsung Health (install / update / setup / Developer Mode) → re-check.
        viewModel.onResume()
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        var showSplash by rememberSaveable { mutableStateOf(true) }
        var openConnect by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        // First run: open Connect Device, unless Samsung Health access is already granted.
        val finishSplash: () -> Unit = {
            scope.launch {
                val current = viewModel.state.value
                if (!settingsRepository.settings.first().onboarded) {
                    if (current.status == DashboardUiState.Status.READY && current.permissions?.allGranted == true) {
                        settingsRepository.update { it.copy(onboarded = true) }
                    } else {
                        openConnect = true
                    }
                }
                showSplash = false
            }
        }
        if (showSplash) {
            LaunchedEffect(Unit) {
                // Long enough for the pop-in, then until the first read finishes (max 4s).
                delay(if (reduceMotion) 400L else 1_400L)
                withTimeoutOrNull(2_600L) {
                    viewModel.state.first { it.status != DashboardUiState.Status.CONNECTING && !it.isRefreshing }
                }
                finishSplash()
            }
            SplashScreen(onOpen = finishSplash)
        } else {
            MainScaffold(viewModel, repository, state, settings, settingsRepository, openConnect, onConnectOpened = { openConnect = false })
        }
    }
}

@Composable
private fun MainScaffold(
    viewModel: DashboardViewModel,
    repository: SamsungHealthRepository,
    state: DashboardUiState,
    settings: UserSettings,
    settingsRepository: SettingsRepository,
    openConnect: Boolean,
    onConnectOpened: () -> Unit,
) {
    val nav = rememberNavController()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    // First run: Connect Device sits on top of Home, so Back lands on Home.
    LaunchedEffect(openConnect) {
        if (openConnect) {
            nav.navigate(Routes.CONNECT)
            onConnectOpened()
        }
    }
    val markOnboarded: () -> Unit = { scope.launch { settingsRepository.update { it.copy(onboarded = true) } } }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    Scaffold(
        containerColor = AfColors.Background,
        bottomBar = { if (Tabs.any { it.route == route }) BottomBar(route) { nav.navigateToTab(it) } },
    ) { padding ->
        val back: () -> Unit = { nav.popBackStack() }
        val allData: () -> Unit = { nav.navigate(Routes.ALL_DATA) }
        NavHost(nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    state = state,
                    settings = settings,
                    actions = HomeActions(
                        onSteps = { nav.navigate(Routes.STEPS) },
                        onHeartRate = { nav.navigate(Routes.HEART_RATE) },
                        onSleep = { nav.navigate(Routes.SLEEP) },
                        onOxygen = { nav.navigate(Routes.OXYGEN) },
                        onWeight = { nav.navigate(Routes.WEIGHT) },
                        onCoach = { nav.navigate(Routes.COACH) },
                        onProfile = { nav.navigate(Routes.PROFILE) },
                        onHealthSync = { nav.navigate(Routes.HEALTH_SYNC) },
                    ),
                    contentPadding = padding,
                )
            }
            composable(Routes.WORKOUT) { ComingSoonScreen("Workout", 5, padding, onBack = null) }
            composable(Routes.NUTRITION) { ComingSoonScreen("Nutrition", 4, padding, onBack = null) }
            composable(Routes.PROGRESS) { ComingSoonScreen("Progress", 6, padding, onBack = null) }
            composable(Routes.STEPS) {
                StepsScreen(state, settings.stepGoal, repository::getDailySteps, padding, onBack = back)
            }
            composable(Routes.HEART_RATE) {
                HeartRateScreen(state, repository::getHeartRateSeries, repository::getDailyHeartRateRange, padding, onBack = back)
            }
            composable(Routes.SLEEP) { SleepScreen(state, startOnOxygen = false, contentPadding = padding, onBack = back) }
            composable(Routes.OXYGEN) { SleepScreen(state, startOnOxygen = true, contentPadding = padding, onBack = back) }
            composable(Routes.WEIGHT) { ComingSoonScreen("Weight tracker", 4, padding, back, allData) }
            composable(Routes.COACH) { ComingSoonScreen("AI coach", 7, padding, back) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    settings = settings,
                    samsungConnected = state.status == DashboardUiState.Status.READY,
                    onUpdate = settingsRepository::update,
                    contentPadding = padding,
                    onBack = back,
                    onConnectedDevices = { nav.navigate(Routes.CONNECT) },
                )
            }
            composable(Routes.HEALTH_SYNC) { HealthSyncScreen(viewModel, state, padding, onBack = back) }
            composable(Routes.CONNECT) {
                ConnectScreen(
                    heartRateBpm = state.data.heartRate.firstOrNull()?.bpm?.roundToInt(),
                    steps = state.data.steps,
                    stepGoal = settings.stepGoal,
                    contentPadding = padding,
                    onBack = {
                        markOnboarded()
                        nav.popBackStack()
                    },
                    onConnect = {
                        activity?.let {
                            viewModel.requestPermissions(it) {
                                markOnboarded()
                                nav.navigate(Routes.HEALTH_SYNC) { popUpTo(Routes.CONNECT) { inclusive = true } }
                            }
                        }
                    },
                )
            }
            composable(Routes.ALL_DATA) {
                Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                    DashboardScreen(viewModel, onBack = back)
                }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    Column(Modifier.background(AfColors.Card)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AfColors.Inner))
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Tabs.forEach { tab ->
                val selected = tab.route == current
                val color = if (selected) AfColors.Accent else AfColors.TextMuted
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { this.selected = selected }
                        .clickable(role = Role.Tab, onClickLabel = tab.label) { onSelect(tab.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = color,
                    )
                }
            }
        }
    }
}
