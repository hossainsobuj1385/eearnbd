package com.example.data.model

data class User(
    val id: Long = 1L,
    val uuid: String = "usr-8a92-f01e",
    val email: String,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val isActive: Boolean = true,
    val isStaff: Boolean = false,
    val isVerified: Boolean = true,
    val createdAt: String = "2026-09-01T10:00:00Z",
    val token: String = "jwt-sec-tok-9921"
) {
    val fullName: String get() = "$firstName $lastName".trim().ifEmpty { email }
}

data class Category(
    val id: Long,
    val name: String,
    val slug: String,
    val icon: String,
    val description: String = "",
    val productCount: Int = 0
)

data class Product(
    val id: Long,
    val name: String,
    val slug: String,
    val sku: String,
    val description: String,
    val shortDescription: String,
    val categoryId: Long,
    val categoryName: String,
    val basePrice: Double,
    val comparePrice: Double,
    val status: String = "ACTIVE", // ACTIVE, INACTIVE, OUT_OF_STOCK
    val mainImage: String,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val rating: Float = 4.8f,
    val reviewCount: Int = 128,
    val variations: List<ProductVariation> = emptyList()
) {
    val discountPercent: Int
        get() = if (comparePrice > basePrice) {
            (((comparePrice - basePrice) / comparePrice) * 100).toInt()
        } else 0
}

data class ProductVariation(
    val id: Long,
    val productId: Long,
    val sku: String,
    val variantName: String, // e.g. "Size: M / Color: Midnight Black"
    val attributeType: String, // "Size", "Color"
    val attributeValue: String, // "M", "Midnight Black"
    val price: Double,
    val stock: Int,
    val reservedStock: Int = 0,
    val isActive: Boolean = true
) {
    val availableStock: Int get() = (stock - reservedStock).coerceAtLeast(0)
}

data class CartItem(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productSlug: String,
    val mainImage: String,
    val variationId: Long?,
    val variantName: String?,
    val unitPrice: Double,
    val quantity: Int,
    val maxStock: Int
) {
    val lineTotal: Double get() = unitPrice * quantity
}

data class WishlistItem(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productSlug: String,
    val mainImage: String,
    val price: Double,
    val comparePrice: Double,
    val inStock: Boolean = true
)

data class Coupon(
    val code: String,
    val discountPercent: Double,
    val maxDiscount: Double,
    val minSpend: Double,
    val description: String,
    val isValid: Boolean = true
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}

data class Order(
    val id: Long,
    val orderNumber: String,
    val userId: Long,
    val subtotal: Double,
    val discount: Double,
    val shippingCost: Double,
    val total: Double,
    val couponCode: String?,
    val status: OrderStatus,
    val paymentStatusPlaceholder: String = "COD (Payment excluded in Phase-1)",
    val shippingAddress: String,
    val createdAt: String,
    val items: List<OrderItem>
)

data class OrderItem(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val sku: String,
    val variantName: String?,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double,
    val total: Double
)

data class AffiliateProfile(
    val userId: Long,
    val affiliateCode: String,
    val status: String, // PENDING, ACTIVE, SUSPENDED
    val approvedAt: String?,
    val totalClicks: Int,
    val uniqueClicks: Int,
    val promotedProductsCount: Int,
    val conversionsPlaceholder: Int = 0,
    val earningsPlaceholder: Double = 0.0
)

data class AffiliateClick(
    val clickId: String,
    val affiliateCode: String,
    val productId: Long,
    val productName: String,
    val referer: String,
    val userAgent: String,
    val ipHash: String,
    val timestamp: String
)

data class AdminDashboardStats(
    val totalUsers: Int,
    val totalProducts: Int,
    val activeProducts: Int,
    val totalOrders: Int,
    val pendingOrders: Int,
    val deliveredOrders: Int,
    val affiliateUsers: Int,
    val affiliateClicks: Int,
    val inventoryValue: Double
)
