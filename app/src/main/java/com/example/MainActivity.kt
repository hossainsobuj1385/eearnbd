package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ApexViewModel
import com.example.ui.Screen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AffiliateDashboardScreen
import com.example.ui.screens.AppBottomNavigationBar
import com.example.ui.screens.AppDownloadScreen
import com.example.ui.screens.AppHeader
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.ApexCommerceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApexCommerceTheme {
                ApexCommerceApp()
            }
        }
    }
}

@Composable
fun ApexCommerceApp(viewModel: ApexViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle system back navigation
    BackHandler(enabled = currentScreen != Screen.HOME) {
        viewModel.navigateBack()
    }

    val totalCartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                currentScreen = currentScreen,
                cartItemCount = totalCartCount,
                onNavigateBack = { viewModel.navigateBack() },
                onNavigateTo = { viewModel.navigateTo(it) }
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                currentScreen = currentScreen,
                cartItemCount = totalCartCount,
                wishlistItemCount = wishlistItems.size,
                onNavigateTo = { viewModel.navigateTo(it) }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding),
            label = "screen_transition"
        ) { targetScreen ->
            when (targetScreen) {
                Screen.HOME -> HomeScreen(viewModel = viewModel)
                Screen.PRODUCTS -> ProductsScreen(viewModel = viewModel)
                Screen.PRODUCT_DETAIL -> ProductDetailScreen(viewModel = viewModel)
                Screen.CART -> CartScreen(viewModel = viewModel)
                Screen.WISHLIST -> WishlistScreen(viewModel = viewModel)
                Screen.CHECKOUT -> CheckoutScreen(viewModel = viewModel)
                Screen.ORDERS -> OrdersScreen(viewModel = viewModel)
                Screen.PROFILE -> ProfileScreen(viewModel = viewModel)
                Screen.AFFILIATE -> AffiliateDashboardScreen(viewModel = viewModel)
                Screen.ADMIN -> AdminDashboardScreen(viewModel = viewModel)
                Screen.APP_DOWNLOAD -> AppDownloadScreen(viewModel = viewModel)
            }
        }
    }
}
