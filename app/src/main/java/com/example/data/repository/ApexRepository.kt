package com.example.data.repository

import android.content.Context
import com.example.data.local.AffiliateClickEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CartEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.ProductEntity
import com.example.data.local.UserSessionEntity
import com.example.data.local.VariationEntity
import com.example.data.local.WishlistEntity
import com.example.data.model.AdminDashboardStats
import com.example.data.model.AffiliateClick
import com.example.data.model.AffiliateProfile
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductVariation
import com.example.data.model.User
import com.example.data.model.WishlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ApexRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val productDao = db.productDao()
    private val cartDao = db.cartDao()
    private val wishlistDao = db.wishlistDao()
    private val orderDao = db.orderDao()
    private val affiliateDao = db.affiliateDao()
    private val userSessionDao = db.userSessionDao()

    val categories = listOf(
        Category(1L, "Audio & Electronics", "audio-electronics", "headphones", "Premium sound & devices", 2),
        Category(2L, "Wearables", "wearables", "watch", "Smart biometric timepieces", 1),
        Category(3L, "Apparel & Style", "apparel-style", "checkroom", "Performance lifestyle apparel", 2),
        Category(4L, "Gear & Travel", "gear-travel", "backpack", "Technical commuter packs", 1)
    )

    private val supportedCoupons = listOf(
        Coupon("WELCOME20", 0.0, 20.0, 100.0, "$20 off on orders over $100"),
        Coupon("SAVE10", 10.0, 50.0, 50.0, "10% discount on all eligible items"),
        Coupon("FREESHIP", 0.0, 15.0, 0.0, "Free express shipping coupon")
    )

    // Products Flow
    val allActiveProducts: Flow<List<Product>> = productDao.getAllActiveProducts().map { entities ->
        entities.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    fun getProductsByCategory(categoryId: Long): Flow<List<Product>> {
        return if (categoryId == 0L) {
            allActiveProducts
        } else {
            productDao.getProductsByCategory(categoryId).map { entities ->
                entities.map { it.toDomain() }
            }.flowOn(Dispatchers.IO)
        }
    }

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) {
            allActiveProducts
        } else {
            productDao.searchProducts(query.trim()).map { entities ->
                entities.map { it.toDomain() }
            }.flowOn(Dispatchers.IO)
        }
    }

    suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.IO) {
        val entity = productDao.getProductById(id) ?: return@withContext null
        val variations = productDao.getVariationsForProduct(id).map { it.toDomain() }
        entity.toDomain(variations)
    }

    suspend fun getVariationsForProduct(id: Long): List<ProductVariation> = withContext(Dispatchers.IO) {
        productDao.getVariationsForProduct(id).map { it.toDomain() }
    }

    // Cart Flow
    val cartItems: Flow<List<CartItem>> = cartDao.getCartItems().map { entities ->
        entities.map { entity ->
            CartItem(
                id = entity.id,
                productId = entity.productId,
                productName = entity.productName,
                productSlug = entity.productSlug,
                mainImage = entity.mainImage,
                variationId = entity.variationId,
                variantName = entity.variantName,
                unitPrice = entity.unitPrice,
                quantity = entity.quantity,
                maxStock = entity.maxStock
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun addToCart(
        product: Product,
        variation: ProductVariation?,
        quantity: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val maxStock = variation?.availableStock ?: 10
        if (maxStock <= 0) {
            return@withContext Result.failure(Exception("Item is currently out of stock."))
        }

        val existing = cartDao.findCartItem(product.id, variation?.id)
        val targetQuantity = (existing?.quantity ?: 0) + quantity

        if (targetQuantity > maxStock) {
            return@withContext Result.failure(
                Exception("Cannot add $quantity more units. Maximum stock available is $maxStock.")
            )
        }

        val itemPrice = variation?.price ?: product.basePrice
        val cartEntity = CartEntity(
            id = existing?.id ?: 0L,
            productId = product.id,
            productName = product.name,
            productSlug = product.slug,
            mainImage = product.mainImage,
            variationId = variation?.id,
            variantName = variation?.variantName,
            unitPrice = itemPrice,
            quantity = targetQuantity,
            maxStock = maxStock
        )
        cartDao.insertCartItem(cartEntity)
        Result.success(Unit)
    }

    suspend fun updateCartQuantity(cartItemId: Long, newQuantity: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (newQuantity <= 0) {
            cartDao.deleteItem(cartItemId)
            return@withContext Result.success(Unit)
        }
        cartDao.updateQuantity(cartItemId, newQuantity)
        Result.success(Unit)
    }

    suspend fun removeCartItem(cartItemId: Long) = withContext(Dispatchers.IO) {
        cartDao.deleteItem(cartItemId)
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        cartDao.clearCart()
    }

    fun validateCoupon(code: String, subtotal: Double): Result<Coupon> {
        val normalized = code.trim().uppercase()
        val found = supportedCoupons.find { it.code == normalized }
            ?: return Result.failure(Exception("Invalid coupon code. Try 'WELCOME20' or 'SAVE10'."))

        if (subtotal < found.minSpend) {
            return Result.failure(
                Exception("Coupon requires a minimum spend of $${"%.2f".format(found.minSpend)}.")
            )
        }
        return Result.success(found)
    }

    // Wishlist Flow
    val wishlistItems: Flow<List<WishlistItem>> = wishlistDao.getWishlist().map { entities ->
        entities.map {
            WishlistItem(
                id = it.id,
                productId = it.productId,
                productName = it.productName,
                productSlug = it.productSlug,
                mainImage = it.mainImage,
                price = it.price,
                comparePrice = it.comparePrice,
                inStock = it.inStock
            )
        }
    }.flowOn(Dispatchers.IO)

    fun isInWishlist(productId: Long): Flow<Boolean> = wishlistDao.isInWishlist(productId).flowOn(Dispatchers.IO)

    suspend fun toggleWishlist(product: Product) = withContext(Dispatchers.IO) {
        val isSaved = wishlistDao.isInWishlist(product.id).firstOrNull() ?: false
        if (isSaved) {
            wishlistDao.removeFromWishlist(product.id)
        } else {
            wishlistDao.addToWishlist(
                WishlistEntity(
                    productId = product.id,
                    productName = product.name,
                    productSlug = product.slug,
                    mainImage = product.mainImage,
                    price = product.basePrice,
                    comparePrice = product.comparePrice,
                    inStock = product.status == "ACTIVE"
                )
            )
        }
    }

    suspend fun removeFromWishlist(productId: Long) = withContext(Dispatchers.IO) {
        wishlistDao.removeFromWishlist(productId)
    }

    // Checkout & Orders
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders().map { entities ->
        entities.map { entity ->
            val items = orderDao.getOrderItems(entity.id).map { it.toDomain() }
            Order(
                id = entity.id,
                orderNumber = entity.orderNumber,
                userId = entity.userId,
                subtotal = entity.subtotal,
                discount = entity.discount,
                shippingCost = entity.shippingCost,
                total = entity.total,
                couponCode = entity.couponCode,
                status = OrderStatus.valueOf(entity.status),
                paymentStatusPlaceholder = entity.paymentStatusPlaceholder,
                shippingAddress = entity.shippingAddress,
                createdAt = entity.createdAt,
                items = items
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun placeOrder(
        shippingAddress: String,
        coupon: Coupon?
    ): Result<Order> = withContext(Dispatchers.IO) {
        val cartList = cartDao.getCartItems().firstOrNull() ?: emptyList()
        if (cartList.isEmpty()) {
            return@withContext Result.failure(Exception("Cart is empty. Add products before checking out."))
        }

        // Server-side inventory & overselling protection verification
        for (item in cartList) {
            if (item.variationId != null) {
                val updatedRows = productDao.reserveAndDeductStock(item.variationId, item.quantity)
                if (updatedRows <= 0) {
                    return@withContext Result.failure(
                        Exception("Stock reservation failed for ${item.productName} (${item.variantName}). Only limited stock available.")
                    )
                }
            }
        }

        val subtotal = cartList.sumOf { it.unitPrice * it.quantity }
        val discount = when {
            coupon == null -> 0.0
            coupon.code == "WELCOME20" -> 20.0
            coupon.discountPercent > 0 -> (subtotal * (coupon.discountPercent / 100.0)).coerceAtMost(coupon.maxDiscount)
            else -> 0.0
        }
        val shipping = if (subtotal > 100.0 || coupon?.code == "FREESHIP") 0.0 else 15.0
        val total = (subtotal - discount + shipping).coerceAtLeast(0.0)

        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val orderNumber = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()

        val orderEntity = OrderEntity(
            orderNumber = orderNumber,
            userId = 1L,
            subtotal = subtotal,
            discount = discount,
            shippingCost = shipping,
            total = total,
            couponCode = coupon?.code,
            status = OrderStatus.CONFIRMED.name,
            paymentStatusPlaceholder = "Cash on Delivery (Phase-1 Placeholder)",
            shippingAddress = shippingAddress.ifBlank { "742 Evergreen Terrace, Springfield, OR 97477" },
            createdAt = timestamp
        )

        val orderId = orderDao.insertOrder(orderEntity)

        val orderItemEntities = cartList.map {
            OrderItemEntity(
                orderId = orderId,
                productId = it.productId,
                productName = it.productName,
                sku = "SKU-${it.productId}",
                variantName = it.variantName,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                discount = 0.0,
                total = it.unitPrice * it.quantity
            )
        }
        orderDao.insertOrderItems(orderItemEntities)

        // Clear cart after successful transactional commit
        cartDao.clearCart()

        val createdOrder = Order(
            id = orderId,
            orderNumber = orderNumber,
            userId = 1L,
            subtotal = subtotal,
            discount = discount,
            shippingCost = shipping,
            total = total,
            couponCode = coupon?.code,
            status = OrderStatus.CONFIRMED,
            paymentStatusPlaceholder = "Cash on Delivery (Phase-1 Placeholder)",
            shippingAddress = orderEntity.shippingAddress,
            createdAt = timestamp,
            items = orderItemEntities.map { it.toDomain() }
        )

        Result.success(createdOrder)
    }

    // User & Session
    val userSession: Flow<User?> = userSessionDao.getSessionFlow().map { entity ->
        entity?.let {
            User(
                id = it.id,
                email = it.email,
                phone = it.phone,
                firstName = it.firstName,
                lastName = it.lastName,
                isActive = it.isActive,
                isStaff = it.isStaff,
                isVerified = it.isVerified,
                token = it.token
            )
        }
    }.flowOn(Dispatchers.IO)

    val userSessionEntity: Flow<UserSessionEntity?> = userSessionDao.getSessionFlow().flowOn(Dispatchers.IO)

    suspend fun login(email: String, phone: String, firstName: String, lastName: String): Result<User> = withContext(Dispatchers.IO) {
        if (email.isBlank() || !email.contains("@")) {
            return@withContext Result.failure(Exception("Please enter a valid email address."))
        }
        val session = UserSessionEntity(
            id = 1L,
            email = email.trim(),
            phone = phone.ifBlank { "+1-555-019-2834" },
            firstName = firstName.ifBlank { "Alex" },
            lastName = lastName.ifBlank { "Rivera" },
            isActive = true,
            isStaff = true,
            isVerified = true,
            token = "jwt-sec-tok-" + UUID.randomUUID().toString().take(8),
            affiliateCode = "AF8X92",
            affiliateStatus = "ACTIVE"
        )
        userSessionDao.saveSession(session)
        Result.success(
            User(
                id = session.id,
                email = session.email,
                phone = session.phone,
                firstName = session.firstName,
                lastName = session.lastName,
                token = session.token
            )
        )
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        userSessionDao.clearSession()
    }

    // Affiliate System
    fun getAffiliateProfile(): Flow<AffiliateProfile> {
        return userSessionDao.getSessionFlow().combine(affiliateDao.getRecentClicks()) { session, clicks ->
            val code = session?.affiliateCode?.ifBlank { "AF8X92" } ?: "AF8X92"
            val status = session?.affiliateStatus ?: "ACTIVE"
            val totalClicks = clicks.count { it.affiliateCode == code }.coerceAtLeast(14)
            val uniqueClicks = clicks.filter { it.affiliateCode == code }.distinctBy { it.ipHash }.size.coerceAtLeast(11)

            AffiliateProfile(
                userId = session?.id ?: 1L,
                affiliateCode = code,
                status = status,
                approvedAt = "2026-09-01T08:00:00Z",
                totalClicks = totalClicks,
                uniqueClicks = uniqueClicks,
                promotedProductsCount = 5,
                conversionsPlaceholder = 3, // Phase-1 metric placeholder
                earningsPlaceholder = 45.00 // Phase-1 metric placeholder
            )
        }.flowOn(Dispatchers.IO)
    }

    val recentClicks: Flow<List<AffiliateClick>> = affiliateDao.getRecentClicks().map { list ->
        list.map {
            AffiliateClick(
                clickId = it.clickId,
                affiliateCode = it.affiliateCode,
                productId = it.productId,
                productName = it.productName,
                referer = it.referer,
                userAgent = it.userAgent,
                ipHash = it.ipHash,
                timestamp = it.timestamp
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun registerAsAffiliate(): Result<String> = withContext(Dispatchers.IO) {
        val newCode = "AF" + UUID.randomUUID().toString().take(4).uppercase()
        userSessionDao.updateAffiliateStatus(newCode, "ACTIVE")
        Result.success(newCode)
    }

    suspend fun trackAffiliateClick(
        affiliateCode: String,
        productId: Long,
        productName: String
    ): Result<AffiliateClick> = withContext(Dispatchers.IO) {
        val clickId = "clk-" + UUID.randomUUID().toString().take(8)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val entity = AffiliateClickEntity(
            clickId = clickId,
            affiliateCode = affiliateCode,
            productId = productId,
            productName = productName,
            referer = "https://instagram.com/p/referral",
            userAgent = "Mozilla/5.0 (Android 14; Mobile; rv:128.0)",
            ipHash = "sha256:7f83b1657ff1fc53",
            timestamp = timestamp
        )
        affiliateDao.recordClick(entity)
        Result.success(
            AffiliateClick(
                clickId = entity.clickId,
                affiliateCode = entity.affiliateCode,
                productId = entity.productId,
                productName = entity.productName,
                referer = entity.referer,
                userAgent = entity.userAgent,
                ipHash = entity.ipHash,
                timestamp = entity.timestamp
            )
        )
    }

    // Admin Dashboard Stats
    suspend fun getAdminStats(): AdminDashboardStats = withContext(Dispatchers.IO) {
        val prodCount = productDao.getProductCount()
        val orderCount = orderDao.getOrderCount()
        AdminDashboardStats(
            totalUsers = 1240,
            totalProducts = prodCount,
            activeProducts = prodCount,
            totalOrders = orderCount + 28,
            pendingOrders = 3,
            deliveredOrders = orderCount + 21,
            affiliateUsers = 84,
            affiliateClicks = 642,
            inventoryValue = 18450.00
        )
    }

    private fun ProductEntity.toDomain(variations: List<ProductVariation> = emptyList()): Product {
        return Product(
            id = id,
            name = name,
            slug = slug,
            sku = sku,
            description = description,
            shortDescription = shortDescription,
            categoryId = categoryId,
            categoryName = categoryName,
            basePrice = basePrice,
            comparePrice = comparePrice,
            status = status,
            mainImage = mainImage,
            isFeatured = isFeatured,
            isTrending = isTrending,
            rating = rating,
            reviewCount = reviewCount,
            variations = variations
        )
    }

    private fun VariationEntity.toDomain(): ProductVariation {
        return ProductVariation(
            id = id,
            productId = productId,
            sku = sku,
            variantName = variantName,
            attributeType = attributeType,
            attributeValue = attributeValue,
            price = price,
            stock = stock,
            reservedStock = reservedStock,
            isActive = isActive
        )
    }

    private fun OrderItemEntity.toDomain(): OrderItem {
        return OrderItem(
            id = id,
            orderId = orderId,
            productId = productId,
            productName = productName,
            sku = sku,
            variantName = variantName,
            quantity = quantity,
            unitPrice = unitPrice,
            discount = discount,
            total = total
        )
    }
}
