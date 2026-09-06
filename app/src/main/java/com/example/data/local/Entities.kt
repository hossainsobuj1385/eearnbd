package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["slug"], unique = true),
        Index(value = ["sku"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["status"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val slug: String,
    val sku: String,
    val description: String,
    val shortDescription: String,
    val categoryId: Long,
    val categoryName: String,
    val basePrice: Double,
    val comparePrice: Double,
    val status: String,
    val mainImage: String,
    val isFeatured: Boolean,
    val isTrending: Boolean,
    val rating: Float,
    val reviewCount: Int
)

@Entity(
    tableName = "product_variations",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["sku"], unique = true)
    ]
)
data class VariationEntity(
    @PrimaryKey val id: Long,
    val productId: Long,
    val sku: String,
    val variantName: String,
    val attributeType: String,
    val attributeValue: String,
    val price: Double,
    val stock: Int,
    val reservedStock: Int,
    val isActive: Boolean
)

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val productSlug: String,
    val mainImage: String,
    val variationId: Long?,
    val variantName: String?,
    val unitPrice: Double,
    val quantity: Int,
    val maxStock: Int
)

@Entity(
    tableName = "wishlist",
    indices = [Index(value = ["productId"], unique = true)]
)
data class WishlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val productSlug: String,
    val mainImage: String,
    val price: Double,
    val comparePrice: Double,
    val inStock: Boolean
)

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderNumber"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["status"])
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val userId: Long,
    val subtotal: Double,
    val discount: Double,
    val shippingCost: Double,
    val total: Double,
    val couponCode: String?,
    val status: String,
    val paymentStatusPlaceholder: String,
    val shippingAddress: String,
    val createdAt: String
)

@Entity(
    tableName = "order_items",
    indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

@Entity(
    tableName = "affiliate_clicks",
    indices = [
        Index(value = ["affiliateCode"]),
        Index(value = ["productId"])
    ]
)
data class AffiliateClickEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clickId: String,
    val affiliateCode: String,
    val productId: Long,
    val productName: String,
    val referer: String,
    val userAgent: String,
    val ipHash: String,
    val timestamp: String
)

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val id: Long = 1L,
    val email: String,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val isActive: Boolean,
    val isStaff: Boolean,
    val isVerified: Boolean,
    val token: String,
    val affiliateCode: String = "",
    val affiliateStatus: String = "NOT_REGISTERED" // NOT_REGISTERED, PENDING, ACTIVE
)
