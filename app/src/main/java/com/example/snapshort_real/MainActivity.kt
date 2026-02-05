package com.example.snapshort_real

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.snapshort_real.ui.detail.DetailScreen
import com.example.snapshort_real.ui.gallery.GalleryScreen
import com.example.snapshort_real.ui.gallery.GalleryViewModel
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SnapShortNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SnapShortNavHost(
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "gallery",
        modifier = modifier
    ) {
        composable("gallery") {
            GalleryScreen(
                viewModel = viewModel,
                onImageClick = { file ->
                    val encodedPath = URLEncoder.encode(file.absolutePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("detail/$encodedPath")
                }
            )
        }
        
        composable(
            route = "detail/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: return@composable
            
            DetailScreen(
                imagePath = imagePath,
                onBackClick = { navController.popBackStack() },
                onDeleteClick = { file ->
                    viewModel.deleteImage(file)
                    navController.popBackStack()
                }
            )
        }
    }
}