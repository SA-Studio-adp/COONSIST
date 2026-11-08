package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Food : Screen("food", "Food", Icons.Default.Restaurant)
    object AiCoach : Screen("ai_coach", "AI Coach", Icons.Default.Psychology)
    object Progress : Screen("progress", "Progress", Icons.Default.Timeline)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    // Sub-screens
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.SportsScore)
    object AiScanner : Screen("ai_scanner", "AI Scanner", Icons.Default.CameraAlt)
    object RecipeBuilder : Screen("recipe_builder", "Recipes", Icons.Default.MenuBook)
    object DietPlanner : Screen("diet_planner", "Diet Plan", Icons.Default.EventNote)
    object WorkoutTracker : Screen("workout_tracker", "Workouts", Icons.Default.FitnessCenter)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Food,
    Screen.AiCoach,
    Screen.Progress,
    Screen.Profile
)

@Composable
fun ConsistAppNavigation(
    viewModel: ConsistViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userProfile by viewModel.userProfile.collectAsState()
    var showVoiceLogDialog by remember { mutableStateOf(false) }

    // Start with onboarding if not yet onboarded
    val startDestination = if (userProfile?.isOnboarded == true) Screen.Home.route else Screen.Onboarding.route

    val isBottomBarVisible = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = ConsistBlack,
                    contentColor = ConsistWhite
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) ConsistCyan else ConsistGrey
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    color = if (isSelected) ConsistCyan else ConsistGrey
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = ConsistCyanContainer
                            )
                        )
                    }
                }
            }
        },
        containerColor = ConsistBlack
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    viewModel = viewModel,
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToFood = { navController.navigate(Screen.Food.route) },
                    onNavigateToAiScan = { navController.navigate(Screen.AiScanner.route) },
                    onNavigateToAiCoach = { navController.navigate(Screen.AiCoach.route) },
                    onNavigateToWorkouts = { navController.navigate(Screen.WorkoutTracker.route) },
                    onNavigateToDietPlan = { navController.navigate(Screen.DietPlanner.route) },
                    onOpenVoiceLog = { showVoiceLogDialog = true }
                )
            }

            composable(Screen.Food.route) {
                FoodScreen(
                    viewModel = viewModel,
                    onNavigateToAiScan = { navController.navigate(Screen.AiScanner.route) },
                    onOpenVoiceLog = { showVoiceLogDialog = true },
                    onNavigateToRecipeBuilder = { navController.navigate(Screen.RecipeBuilder.route) },
                    onNavigateToDietPlan = { navController.navigate(Screen.DietPlanner.route) }
                )
            }

            composable(Screen.AiCoach.route) {
                AiCoachScreen(viewModel = viewModel)
            }

            composable(Screen.Progress.route) {
                ProgressScreen(viewModel = viewModel)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onEditProfile = {
                        navController.navigate(Screen.Onboarding.route)
                    }
                )
            }

            // Sub-destinations
            composable(Screen.AiScanner.route) {
                AiScannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.RecipeBuilder.route) {
                RecipeBuilderScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DietPlanner.route) {
                DietPlannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.WorkoutTracker.route) {
                WorkoutScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Voice Log Dialog overlay
        if (showVoiceLogDialog) {
            VoiceLoggingDialog(
                viewModel = viewModel,
                onDismiss = { showVoiceLogDialog = false }
            )
        }
    }
}
