package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        VariationEntity::class,
        CartEntity::class,
        WishlistEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        AffiliateClickEntity::class,
        UserSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun orderDao(): OrderDao
    abstract fun affiliateDao(): AffiliateDao
    abstract fun userSessionDao(): UserSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apex_commerce.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedInitialData(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(db: AppDatabase) {
            val initialProducts = listOf(
                ProductEntity(
                    id = 101L,
                    name = "AeroPro Wireless Noise-Cancelling Headphones",
                    slug = "aeropro-wireless-headphones",
                    sku = "AUD-APRO-01",
                    description = "Engineered with 45mm custom dynamic neodymium drivers, active hybrid noise cancellation up to 42dB, ultra-low latency game mode, and 50-hour continuous playback with fast USB-C charge.",
                    shortDescription = "Hybrid 42dB ANC, 50hr battery, LDAC Hi-Res Audio.",
                    categoryId = 1L,
                    categoryName = "Audio & Electronics",
                    basePrice = 179.99,
                    comparePrice = 249.99,
                    status = "ACTIVE",
                    mainImage = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80",
                    isFeatured = true,
                    isTrending = true,
                    rating = 4.9f,
                    reviewCount = 342
                ),
                ProductEntity(
                    id = 102L,
                    name = "Vanguard Chrono Smartwatch Ultra",
                    slug = "vanguard-chrono-smartwatch-ultra",
                    sku = "WCH-VNG-02",
                    description = "Aerospace grade titanium bezel, sapphire crystal display, dual-band GPS telemetry, continuous biometric ECG & SpO2 tracking, and 14 days expedition battery life with 100m water resistance.",
                    shortDescription = "Titanium case, Dual GPS, Sapphire Crystal, 100m water resistant.",
                    categoryId = 2L,
                    categoryName = "Wearables",
                    basePrice = 299.00,
                    comparePrice = 379.00,
                    status = "ACTIVE",
                    mainImage = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80",
                    isFeatured = true,
                    isTrending = true,
                    rating = 4.8f,
                    reviewCount = 189
                ),
                ProductEntity(
                    id = 103L,
                    name = "Apex Minimalist Merino Wool Hoodie",
                    slug = "apex-minimalist-merino-hoodie",
                    sku = "APP-MRN-03",
                    description = "Spun from 100% superfine 18.5-micron New Zealand Merino wool with temperature-regulating microfibers, concealed YKK zip pockets, and ergonomic articulation for everyday mobility.",
                    shortDescription = "100% Superfine Merino Wool, moisture wicking, thermal comfort.",
                    categoryId = 3L,
                    categoryName = "Apparel & Style",
                    basePrice = 119.50,
                    comparePrice = 145.00,
                    status = "ACTIVE",
                    mainImage = "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=600&q=80",
                    isFeatured = false,
                    isTrending = true,
                    rating = 4.7f,
                    reviewCount = 95
                ),
                ProductEntity(
                    id = 104L,
                    name = "Luminary Mechanical Dual-Mode Keyboard",
                    slug = "luminary-mechanical-keyboard",
                    sku = "PC-LMN-04",
                    description = "CNC milled aluminum chassis, hot-swappable tactile Gateron Pro switches, per-key RGB backlighting, gasket mount structure with acoustic dampening silicone foam, and triple connectivity.",
                    shortDescription = "CNC Aluminum, Hot-swap tactile switches, Gasket mounted.",
                    categoryId = 1L,
                    categoryName = "Audio & Electronics",
                    basePrice = 149.00,
                    comparePrice = 189.00,
                    status = "ACTIVE",
                    mainImage = "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80",
                    isFeatured = true,
                    isTrending = false,
                    rating = 4.9f,
                    reviewCount = 214
                ),
                ProductEntity(
                    id = 105L,
                    name = "Kinetix All-Weather Commuter Backpack",
                    slug = "kinetix-all-weather-backpack",
                    sku = "BAG-KTX-05",
                    description = "Constructed with X-Pac weatherproof laminate, Fidlock magnetic sternum buckle, padded 16-inch laptop compartment, luggage pass-through sleeve, and 24L expandable volume.",
                    shortDescription = "Weatherproof X-Pac fabric, Fidlock buckles, 16-inch laptop vault.",
                    categoryId = 4L,
                    categoryName = "Gear & Travel",
                    basePrice = 135.00,
                    comparePrice = 160.00,
                    status = "ACTIVE",
                    mainImage = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80",
                    isFeatured = false,
                    isTrending = true,
                    rating = 4.8f,
                    reviewCount = 112
                ),
                ProductEntity(
                    id = 106L,
                    name = "Obsidian Studio Polarized Sunglasses",
                    slug = "obsidian-studio-polarized-sunglasses",
                    sku = "ACC-OBS-06",
                    description = "Handcrafted Italian acetate frames with Japanese polarized nylon lenses offering 100% UVA/UVB protection, anti-reflective coating, and 5-barrel custom stainless steel hinges.",
                    shortDescription = "Italian acetate, Polarized UV400 lenses, stainless steel core.",
                    categoryId = 3L,
                    categoryName = "Apparel & Style",
                    basePrice = 89.00,
                    comparePrice = 120.00,
                    status = "ACTIVE",
                    mainImage = "https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=600&q=80",
                    isFeatured = true,
                    isTrending = false,
                    rating = 4.6f,
                    reviewCount = 67
                )
            )
            db.productDao().insertProducts(initialProducts)

            val initialVariations = listOf(
                // 101 - AeroPro Headphones
                VariationEntity(1011L, 101L, "AUD-APRO-01-BLK", "Color: Stealth Black", "Color", "Stealth Black", 179.99, 25, 2, true),
                VariationEntity(1012L, 101L, "AUD-APRO-01-SLV", "Color: Arctic Silver", "Color", "Arctic Silver", 179.99, 14, 0, true),
                VariationEntity(1013L, 101L, "AUD-APRO-01-NVY", "Color: Midnight Navy", "Color", "Midnight Navy", 189.99, 8, 1, true),

                // 102 - Vanguard Smartwatch
                VariationEntity(1021L, 102L, "WCH-VNG-02-44T", "Size: 44mm / Titanium", "Size", "44mm", 299.00, 18, 0, true),
                VariationEntity(1022L, 102L, "WCH-VNG-02-48T", "Size: 48mm / Titanium", "Size", "48mm", 329.00, 10, 1, true),

                // 103 - Merino Hoodie
                VariationEntity(1031L, 103L, "APP-MRN-03-S", "Size: S / Slate Grey", "Size", "S", 119.50, 15, 0, true),
                VariationEntity(1032L, 103L, "APP-MRN-03-M", "Size: M / Slate Grey", "Size", "M", 119.50, 20, 2, true),
                VariationEntity(1033L, 103L, "APP-MRN-03-L", "Size: L / Slate Grey", "Size", "L", 119.50, 12, 0, true),
                VariationEntity(1034L, 103L, "APP-MRN-03-XL", "Size: XL / Slate Grey", "Size", "XL", 124.50, 6, 0, true),

                // 104 - Mechanical Keyboard
                VariationEntity(1041L, 104L, "PC-LMN-04-RED", "Switch: Linear Red", "Switch", "Linear Red", 149.00, 12, 0, true),
                VariationEntity(1042L, 104L, "PC-LMN-04-BRN", "Switch: Tactile Brown", "Switch", "Tactile Brown", 149.00, 15, 1, true),

                // 105 - Commuter Backpack
                VariationEntity(1051L, 105L, "BAG-KTX-05-BLK", "Color: Matte Carbon", "Color", "Matte Carbon", 135.00, 22, 1, true),
                VariationEntity(1052L, 105L, "BAG-KTX-05-OLV", "Color: Military Olive", "Color", "Military Olive", 135.00, 9, 0, true),

                // 106 - Polarized Sunglasses
                VariationEntity(1061L, 106L, "ACC-OBS-06-GLD", "Frame: Gold / Tortoise", "Style", "Gold / Tortoise", 89.00, 14, 0, true),
                VariationEntity(1062L, 106L, "ACC-OBS-06-BLK", "Frame: Matte Onyx", "Style", "Matte Onyx", 89.00, 19, 0, true)
            )
            db.productDao().insertVariations(initialVariations)

            // Seed an initial user session
            val defaultUser = UserSessionEntity(
                id = 1L,
                email = "alex.rivera@example.com",
                phone = "+1-555-019-2834",
                firstName = "Alex",
                lastName = "Rivera",
                isActive = true,
                isStaff = true,
                isVerified = true,
                token = "jwt-sec-tok-9921",
                affiliateCode = "AF8X92",
                affiliateStatus = "ACTIVE"
            )
            db.userSessionDao().saveSession(defaultUser)

            // Seed initial order history
            val seededOrder = OrderEntity(
                id = 1L,
                orderNumber = "ORD-2026-9901",
                userId = 1L,
                subtotal = 179.99,
                discount = 20.00,
                shippingCost = 0.00,
                total = 159.99,
                couponCode = "WELCOME20",
                status = "SHIPPED",
                paymentStatusPlaceholder = "COD (Payment excluded in Phase-1)",
                shippingAddress = "742 Evergreen Terrace, Springfield, OR 97477",
                createdAt = "2026-09-02T14:32:00Z"
            )
            db.orderDao().insertOrder(seededOrder)
            db.orderDao().insertOrderItems(listOf(
                OrderItemEntity(
                    id = 1L,
                    orderId = 1L,
                    productId = 101L,
                    productName = "AeroPro Wireless Noise-Cancelling Headphones",
                    sku = "AUD-APRO-01-BLK",
                    variantName = "Color: Stealth Black",
                    quantity = 1,
                    unitPrice = 179.99,
                    discount = 20.00,
                    total = 159.99
                )
            ))
        }
    }
}
