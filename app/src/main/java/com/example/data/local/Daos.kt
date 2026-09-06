package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY isFeatured DESC, id ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND status = 'ACTIVE'")
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE status = 'ACTIVE' AND (
            name LIKE '%' || :query || '%' OR 
            shortDescription LIKE '%' || :query || '%' OR 
            description LIKE '%' || :query || '%' OR 
            categoryName LIKE '%' || :query || '%'
        )
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Query("SELECT * FROM product_variations WHERE productId = :productId AND isActive = 1")
    suspend fun getVariationsForProduct(productId: Long): List<VariationEntity>

    @Query("SELECT * FROM product_variations WHERE productId = :productId AND isActive = 1")
    fun getVariationsFlow(productId: Long): Flow<List<VariationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariations(variations: List<VariationEntity>)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Query("UPDATE product_variations SET stock = stock - :qty WHERE id = :variationId AND (stock - reservedStock) >= :qty")
    suspend fun reserveAndDeductStock(variationId: Long, qty: Int): Int
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    fun getCartItems(): Flow<List<CartEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId AND (variationId = :variationId OR (:variationId IS NULL AND variationId IS NULL)) LIMIT 1")
    suspend fun findCartItem(productId: Long, variationId: Long?): CartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartEntity): Long

    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Int)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist ORDER BY id DESC")
    fun getWishlist(): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWishlist(item: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE productId = :productId")
    suspend fun removeFromWishlist(productId: Long)

    @Query("SELECT COUNT(*) > 0 FROM wishlist WHERE productId = :productId")
    fun isInWishlist(productId: Long): Flow<Boolean>
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: Long): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String)

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int
}

@Dao
interface AffiliateDao {
    @Query("SELECT * FROM affiliate_clicks ORDER BY id DESC LIMIT 50")
    fun getRecentClicks(): Flow<List<AffiliateClickEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordClick(click: AffiliateClickEntity)

    @Query("SELECT COUNT(*) FROM affiliate_clicks WHERE affiliateCode = :code")
    fun getTotalClicks(code: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT ipHash) FROM affiliate_clicks WHERE affiliateCode = :code")
    fun getUniqueClicks(code: String): Flow<Int>
}

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    fun getSessionFlow(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    suspend fun getSession(): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSessionEntity)

    @Query("UPDATE user_session SET affiliateCode = :code, affiliateStatus = :status WHERE id = 1")
    suspend fun updateAffiliateStatus(code: String, status: String)

    @Query("DELETE FROM user_session")
    suspend fun clearSession()
}
