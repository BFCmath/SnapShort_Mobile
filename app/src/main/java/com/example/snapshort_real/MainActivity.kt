package com.example.snapshort_real

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.snapshort_real.ui.detail.TaskDetailScreen
import com.example.snapshort_real.ui.gallery.GalleryScreen
import com.example.snapshort_real.ui.tasks.TasksScreen
import com.example.snapshort_real.ui.theme.Snapshort_realTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Snapshort_realTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    val items = listOf("Snaps", "Tasks")
    val icons = listOf(Icons.Default.Home, Icons.Default.List)
    val routes = listOf("gallery", "tasks")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Hide bottom bar on detail screen
            if (currentDestination?.route?.startsWith("task_detail") == false) {
                NavigationBar {
                    items.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = screen) },
                            label = { Text(screen) },
                            selected = currentDestination.hierarchy.any { it.route == routes[index] },
                            onClick = {
                                navController.navigate(routes[index]) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "gallery",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("gallery") {
                GalleryScreen(
                    onImageClick = { file ->
                        val encodedPath = URLEncoder.encode(file.absolutePath, StandardCharsets.UTF_8.toString())
                        navController.navigate("task_detail?imagePath=$encodedPath")
                    }
                )
            }
            
            composable("tasks") {
                TasksScreen(
                    onTaskClick = { taskId ->
                        navController.navigate("task_detail?taskId=$taskId")
                    }
                )
            }
            
            composable(
                route = "task_detail?taskId={taskId}&imagePath={imagePath}",
                arguments = listOf(
                    navArgument("taskId") { 
                        type = NavType.LongType 
                        defaultValue = -1L
                    },
                    navArgument("imagePath") { 
                        type = NavType.StringType 
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getLong("taskId")
                val imagePath = backStackEntry.arguments?.getString("imagePath")
                
                TaskDetailScreen(
                    taskId = taskId,
                    imagePath = imagePath,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}