package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminDashboardStats
import com.example.data.model.AffiliateClick
import com.example.data.model.AffiliateProfile
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.ProductVariation
import com.example.data.model.User
import com.example.data.model.WishlistItem
import com.example.data.repository.ApexRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    PRODUCTS,
    PRODUCT_DETAIL,
    CART,
    WISHLIST,
    CHECKOUT,
    ORDERS,
    PROFILE,
    AFFILIATE,
    ADMIN,
    APP_DOWNLOAD
}

enum class ProductSort {
    POPULAR,
    PRICE_LOW_HIGH,
    PRICE_HIGH_LOW,
    RATING
}

class ApexViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ApexRepository(application)

    // Navigation
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.size - 1)
            true
        } else {
            if (_currentScreen.value != Screen.HOME) {
                _currentScreen.value = Screen.HOME
                true
            } else {
                false
            }
        }
    }

    // Categories
    val categories: List<Category> = repository.categories

    private val _selectedCategoryId = MutableStateFlow(0L)
    val selectedCategoryId: StateFlow<Long> = _selectedCategoryId.asStateFlow()

    fun selectCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    // Search and Sort
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSort.POPULAR)
    val sortOption: StateFlow<ProductSort> = _sortOption.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: ProductSort) {
        _sortOption.value = sort
    }

    // Filtered & Sorted Products
    val products: StateFlow<List<Product>> = combine(
        repository.allActiveProducts,
        _selectedCategoryId,
        _searchQuery,
        _sortOption
    ) { allProds, catId, query, sort ->
        var list = allProds
        if (catId != 0L) {
            list = list.filter { it.categoryId == catId }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.shortDescription.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q)
            }
        }
        when (sort) {
            ProductSort.POPULAR -> list.sortedByDescending { it.reviewCount }
            ProductSort.PRICE_LOW_HIGH -> list.sortedBy { it.basePrice }
            ProductSort.PRICE_HIGH_LOW -> list.sortedByDescending { it.basePrice }
            ProductSort.RATING -> list.sortedByDescending { it.rating }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Product Detail
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _productVariations = MutableStateFlow<List<ProductVariation>>(emptyList())
    val productVariations: StateFlow<List<ProductVariation>> = _productVariations.asStateFlow()

    private val _selectedVariation = MutableStateFlow<ProductVariation?>(null)
    val selectedVariation: StateFlow<ProductVariation?> = _selectedVariation.asStateFlow()

    private val _detailQuantity = MutableStateFlow(1)
    val detailQuantity: StateFlow<Int> = _detailQuantity.asStateFlow()

    private val _isImageZoomed = MutableStateFlow(false)
    val isImageZoomed: StateFlow<Boolean> = _isImageZoomed.asStateFlow()

    fun openProductDetail(product: Product) {
        _selectedProduct.value = product
        _detailQuantity.value = 1
        _isImageZoomed.value = false
        viewModelScope.launch {
            val vars = repository.getVariationsForProduct(product.id)
            _productVariations.value = vars
            _selectedVariation.value = vars.firstOrNull()
        }
        navigateTo(Screen.PRODUCT_DETAIL)
    }

    fun selectVariation(variation: ProductVariation) {
        _selectedVariation.value = variation
        _detailQuantity.value = 1
    }

    fun setDetailQuantity(qty: Int) {
        val maxStock = _selectedVariation.value?.availableStock ?: 10
        _detailQuantity.value = qty.coerceIn(1, maxStock.coerceAtLeast(1))
    }

    fun toggleImageZoom() {
        _isImageZoomed.value = !_isImageZoomed.value
    }

    // Cart
    val cartItems: StateFlow<List<CartItem>> = repository.cartItems.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _couponInput = MutableStateFlow("")
    val couponInput: StateFlow<String> = _couponInput.asStateFlow()

    fun setCouponInput(code: String) {
        _couponInput.value = code
    }

    fun applyCoupon() {
        val subtotal = cartItems.value.sumOf { it.unitPrice * it.quantity }
        val result = repository.validateCoupon(_couponInput.value, subtotal)
        result.onSuccess { coupon ->
            _appliedCoupon.value = coupon
            showSnackbar("Coupon '${coupon.code}' applied successfully!")
        }.onFailure { err ->
            showSnackbar(err.message ?: "Invalid coupon")
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
        showSnackbar("Coupon removed")
    }

    fun addToCart(product: Product, variation: ProductVariation?, quantity: Int) {
        viewModelScope.launch {
            val result = repository.addToCart(product, variation, quantity)
            result.onSuccess {
                showSnackbar("Added '${product.name}' to cart!")
            }.onFailure { err ->
                showSnackbar(err.message ?: "Could not add to cart")
            }
        }
    }

    fun updateCartQuantity(cartItemId: Long, newQty: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, newQty)
        }
    }

    fun removeCartItem(cartItemId: Long) {
        viewModelScope.launch {
            repository.removeCartItem(cartItemId)
            showSnackbar("Item removed from cart")
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
            showSnackbar("Cart cleared")
        }
    }

    // Wishlist
    val wishlistItems: StateFlow<List<WishlistItem>> = repository.wishlistItems.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            repository.toggleWishlist(product)
            showSnackbar("Wishlist updated")
        }
    }

    fun removeFromWishlist(productId: Long) {
        viewModelScope.launch {
            repository.removeFromWishlist(productId)
            showSnackbar("Removed from wishlist")
        }
    }

    fun moveToCart(item: WishlistItem) {
        viewModelScope.launch {
            val product = repository.getProductById(item.productId)
            if (product != null) {
                val vars = repository.getVariationsForProduct(product.id)
                repository.addToCart(product, vars.firstOrNull(), 1)
                repository.removeFromWishlist(item.productId)
                showSnackbar("Moved '${item.productName}' to cart")
            }
        }
    }

    // Checkout & Orders
    private val _shippingAddress = MutableStateFlow("742 Evergreen Terrace, Springfield, OR 97477")
    val shippingAddress: StateFlow<String> = _shippingAddress.asStateFlow()

    private val _isPlacingOrder = MutableStateFlow(false)
    val isPlacingOrder: StateFlow<Boolean> = _isPlacingOrder.asStateFlow()

    fun setShippingAddress(address: String) {
        _shippingAddress.value = address
    }

    val orders: StateFlow<List<Order>> = repository.allOrders.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun placeOrder() {
        if (_isPlacingOrder.value) return
        _isPlacingOrder.value = true
        viewModelScope.launch {
            val res = repository.placeOrder(_shippingAddress.value, _appliedCoupon.value)
            _isPlacingOrder.value = false
            res.onSuccess { order ->
                _appliedCoupon.value = null
                showSnackbar("Order #${order.orderNumber} confirmed!")
                navigateTo(Screen.ORDERS)
            }.onFailure { err ->
                showSnackbar(err.message ?: "Failed to place order.")
            }
        }
    }

    // Affiliate
    val affiliateProfile: StateFlow<AffiliateProfile> = repository.getAffiliateProfile().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        AffiliateProfile(1L, "AF8X92", "ACTIVE", null, 14, 11, 5)
    )

    val recentClicks: StateFlow<List<AffiliateClick>> = repository.recentClicks.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun registerAsAffiliate() {
        viewModelScope.launch {
            val res = repository.registerAsAffiliate()
            res.onSuccess { code ->
                showSnackbar("Affiliate program activated! Your unique code: $code")
            }
        }
    }

    fun generateAffiliateLink(productId: Long): String {
        val code = affiliateProfile.value.affiliateCode
        return "https://apexcommerce.internal/redirect?ref=$code&pid=$productId"
    }

    fun simulateAffiliateClick(product: Product) {
        viewModelScope.launch {
            val code = affiliateProfile.value.affiliateCode
            val res = repository.trackAffiliateClick(code, product.id, product.name)
            res.onSuccess {
                showSnackbar("Tracked affiliate click for ${product.name} (Code: $code)")
            }
        }
    }

    // Admin Stats
    private val _adminStats = MutableStateFlow<AdminDashboardStats?>(null)
    val adminStats: StateFlow<AdminDashboardStats?> = _adminStats.asStateFlow()

    fun loadAdminStats() {
        viewModelScope.launch {
            _adminStats.value = repository.getAdminStats()
        }
    }

    // User Session
    val userSession: StateFlow<User?> = repository.userSession.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        User(1L, "usr-8a92-f01e", "alex.rivera@example.com", "+1-555-019-2834", "Alex", "Rivera")
    )

    fun loginUser(email: String, phone: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            val res = repository.login(email, phone, firstName, lastName)
            res.onSuccess {
                showSnackbar("Welcome back, ${it.firstName}!")
                navigateTo(Screen.PROFILE)
            }.onFailure { err ->
                showSnackbar(err.message ?: "Authentication failed")
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            repository.logout()
            showSnackbar("Signed out successfully")
            navigateTo(Screen.HOME)
        }
    }

    // Snackbar notifications
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private fun showSnackbar(msg: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(msg)
        }
    }
}
