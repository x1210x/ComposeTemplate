package com.example.composetemplate.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.composetemplate.R
import com.example.composetemplate.ui.home.tab1.Tab1Screen
import com.example.composetemplate.ui.home.tab2.Tab2Screen
import com.example.composetemplate.ui.home.tab3.Tab3Screen
import com.example.composetemplate.ui.home.tab4.Tab4Screen
import com.example.composetemplate.ui.widget.DefaultSnackbar
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.launch
import timber.log.Timber

// 하단탭 관련
sealed class Screen(
    @DrawableRes val icon: Int,
    val route: String,
    @StringRes val resourceId: Int,
    val content: (@Composable (HomeViewModel, (String) -> Unit, (String) -> Unit, (String) -> Unit) -> Unit)
) {
    object Tab1 : Screen(R.drawable.ic_place, "tab1", R.string.tab1, { homeViewModel, showSnackbar, navigate, onDispose ->
        Tab1Screen(homeViewModel, showSnackbar = showSnackbar, navigate = navigate, onDispose = onDispose)
    })
    object Tab2 : Screen(R.drawable.ic_chat, "tab2", R.string.tab2, { homeViewModel, showSnackbar, navigate, onDispose ->
        Tab2Screen(homeViewModel, showSnackbar = showSnackbar, navigate = navigate, onDispose = onDispose)
    })
    object Tab3 : Screen(R.drawable.ic_camera, "tab3", R.string.tab3, { homeViewModel, showSnackbar, navigate, onDispose ->
        Tab3Screen(homeViewModel, showSnackbar = showSnackbar, navigate = navigate, onDispose = onDispose)
    })
    object Tab4 : Screen(R.drawable.ic_payment, "tab4", R.string.tab4, { homeViewModel, showSnackbar, navigate, onDispose ->
        Tab4Screen(homeViewModel, showSnackbar = showSnackbar, navigate = navigate, onDispose = onDispose)
    })
}

val ITEMS = listOf(Screen.Tab1, Screen.Tab2, Screen.Tab3, Screen.Tab4)
val START_DESTINATION = Screen.Tab1

// 백 키 관련
const val BACK_PRESS_DELAY_TIME: Long = 2000
var backKeyPressedTime: Long = 0
var toast: Toast? = null

// 하단탭에 대한 네비게이션만 처리
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    navigate: (String) -> Unit,
    showToast: (String) -> Toast,
    onBack: () -> Unit
) {
    val navController = rememberAnimatedNavController()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    // 백키 2회에 종료 처리
    BackCloseHandler(navController, showToast, onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { MyTopAppBar() },
        bottomBar = {
            MyBottomNavigation(navController, ITEMS) { route ->
                viewModel.reselect(route)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HomeNavHost(
                viewModel = viewModel,
                navController = navController,
                items = ITEMS,
                startDestination = START_DESTINATION,
                navigate = navigate,
                showSnackbar = { text ->
                    // showSnackbar
                    scope.launch {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                        scaffoldState.snackbarHostState.showSnackbar(message = text)
                    }
                },
                onDispose = { route ->
                    Timber.d("[템플릿] ${route}.onDispose()")
                    // 재선택 -> 다른탭 -> 해당탭 이동 시 다시 재선택된 것 처럼 동작하여 아래라인 필요
                    viewModel.reselect("")
                }
            )
            DefaultSnackbar(
                snackbarHostState = scaffoldState.snackbarHostState,
                onDismiss = { scaffoldState.snackbarHostState.currentSnackbarData?.dismiss() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 백키 2회에 종료 처리
 */
@Composable
fun BackCloseHandler(
    navController: NavHostController,
    showToast: (String) -> Toast,
    onBack: () -> Unit
) = BackHandler {
    if (!navController.popBackStack()) {
        if (System.currentTimeMillis() > backKeyPressedTime + BACK_PRESS_DELAY_TIME) {
            backKeyPressedTime = System.currentTimeMillis()
            toast = showToast("\'뒤로\' 버튼 한번 더 누르시면 종료됩니다.")
            return@BackHandler
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + BACK_PRESS_DELAY_TIME) {
            toast?.cancel()
            onBack.invoke()
        }
    }
}

@Composable
fun MyTopAppBar() = TopAppBar(
    title = { Text(stringResource(R.string.app_name)) },
    navigationIcon = {
        IconButton(onClick = { }) {
            Icon(Icons.Default.Menu, "Menu")
        }
    }
)

@Composable
fun MyBottomNavigation(navController: NavHostController, items: List<Screen>, onReselet: (String) -> Unit) = BottomNavigation {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    items.forEach { screen ->
        BottomNavigationItem(
            icon = { Icon(painterResource(screen.icon), null) },
            label = { Text(stringResource(screen.resourceId)) },
            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
            onClick = {
                // 다른탭으로 이동할때만 네비게이션
                val from = currentDestination?.route
                val to = screen.route
                if (from != to) {
                    navController.navigate(screen.route) {
                        popUpTo(0) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    onReselet.invoke(currentDestination.route!!)
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeNavHost(
    viewModel: HomeViewModel,
    navController: NavHostController,
    items: List<Screen>,
    startDestination: Screen,
    navigate: (String) -> Unit,
    showSnackbar: (String) -> Unit,
    onDispose: (String) -> Unit
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination.route
    ) {
        items.forEach { screen ->
            composable(
                route = screen.route,
                enterTransition = {
                    val from = initialState.destination.route?.substring(3)?.toInt() ?: 0
                    val to = screen.route.substring(3).toInt()
                    slideInHorizontally(initialOffsetX = { fullWidth -> if (from < to) fullWidth else -fullWidth })
                },
                exitTransition = {
                    val from = screen.route.substring(3).toInt()
                    val to = targetState.destination.route?.substring(3)?.toInt() ?: 0
                    slideOutHorizontally(targetOffsetX = { fullWidth -> if (from < to) -fullWidth else fullWidth })
                }
            ) {
                screen.content.invoke(
                    viewModel,
                    // showSnackbar
                    { text -> showSnackbar(text) },
                    // 상세화면 네비게이션
                    { route -> navigate.invoke(route) },
                    onDispose
                )
            }
        }
    }
}