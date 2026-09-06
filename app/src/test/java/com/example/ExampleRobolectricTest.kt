package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import com.example.data.model.ProductVariation
import com.example.data.repository.ApexRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: ApexRepository

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.AppDatabase.getInstance(context)
        com.example.data.local.AppDatabase.seedInitialData(db)
        repository = ApexRepository(context)
    }

    @Test
    fun `verify app name resource`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Apex Commerce", appName)
    }

    @Test
    fun `verify coupon validation logic`() {
        val validResult = repository.validateCoupon("WELCOME20", 150.0)
        assertTrue(validResult.isSuccess)
        assertEquals("WELCOME20", validResult.getOrNull()?.code)

        val invalidResult = repository.validateCoupon("INVALID99", 150.0)
        assertTrue(invalidResult.isFailure)

        val belowMinSpend = repository.validateCoupon("WELCOME20", 50.0)
        assertTrue(belowMinSpend.isFailure)
    }

    @Test
    fun `verify affiliate tracking logic`() = runBlocking {
        val clickRes = repository.trackAffiliateClick("AF8X92", 101L, "AeroPro Headphones")
        assertTrue(clickRes.isSuccess)
        val click = clickRes.getOrNull()
        assertNotNull(click)
        assertEquals("AF8X92", click?.affiliateCode)
        assertEquals(101L, click?.productId)
    }

    @Test
    fun `verify cart and order checkout transaction`() = runBlocking {
        // Use seeded product 101L and variation 1011L (AeroPro Headphones)
        val testProduct = repository.getProductById(101L)
        assertNotNull(testProduct)
        val variations = repository.getVariationsForProduct(101L)
        assertTrue(variations.isNotEmpty())
        val testVariation = variations.first()

        // Add to cart
        val addRes = repository.addToCart(testProduct!!, testVariation, 1)
        assertTrue(addRes.isSuccess)

        val cartList = repository.cartItems.first()
        assertTrue(cartList.isNotEmpty())

        // Place order (Payment excluded in Phase-1)
        val orderRes = repository.placeOrder("123 Test Street", null)
        assertTrue("Expected order success but was: ${orderRes.exceptionOrNull()?.message}", orderRes.isSuccess)
        val order = orderRes.getOrNull()
        assertNotNull(order)
        assertTrue(order!!.orderNumber.startsWith("ORD-"))
        assertEquals("Cash on Delivery (Phase-1 Placeholder)", order.paymentStatusPlaceholder)
    }
}
