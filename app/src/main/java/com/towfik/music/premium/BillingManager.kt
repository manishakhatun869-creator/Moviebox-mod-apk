package com.towfik.music.premium

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Thin wrapper around Google Play Billing that drives [PremiumRepository].
 * Uses the stable callback-based BillingClient API. Premium is granted only
 * from purchases Play reports as purchased, so it can never be faked locally.
 */
class BillingManager(
    context: Context,
    private val premium: PremiumRepository
) : PurchasesUpdatedListener {

    interface Callback {
        fun onPlans(plans: Map<Plan, ProductDetails>) {}
        fun onPurchaseStarted() {}
        fun onPurchaseSucceeded(plan: Plan) {}
        fun onPurchaseCancelled() {}
        fun onPurchaseFailed() {}
        fun onRestoreDone(found: Boolean) {}
        fun onBillingUnavailable() {}
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val appContext = context.applicationContext

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var plans: Map<Plan, ProductDetails> = emptyMap()
    private var connected = false

    fun connect(callback: Callback) {
        if (connected) { refresh(callback); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connected = true
                    refresh(callback)
                } else {
                    callback.onBillingUnavailable()
                }
            }

            override fun onBillingServiceDisconnected() {
                connected = false
            }
        })
    }

    fun refresh(callback: Callback) {
        scope.launch {
            val found = queryPlans()
            plans = found
            callback.onPlans(found)
            restoreEntitlement()
        }
    }

    fun purchase(activity: Activity, plan: Plan, callback: Callback) {
        val details = plans[plan]
        if (details == null) {
            callback.onBillingUnavailable()
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            callback.onPurchaseStarted()
        } else {
            callback.onPurchaseFailed()
        }
    }

    fun restore(callback: Callback) {
        scope.launch {
            val purchases = queryAllPurchases()
            handlePurchases(purchases)
            val found = purchases.isNotEmpty()
            if (!found) premium.clear()
            withContext(Dispatchers.Main) { callback.onRestoreDone(found) }
        }
    }

    fun shutdown() {
        scope.cancel()
        client.endConnection()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handlePurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> { /* UI reacts via callback */ }
        }
    }

    // ---- internals ----

    private suspend fun queryProductDetails(type: String): List<ProductDetails> =
        suspendCancellableCoroutine { cont ->
            val products = Plan.entries.map { plan ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(plan.productId)
                    .setProductType(type)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
            client.queryProductDetailsAsync(params) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(list.orEmpty())
                } else {
                    cont.resume(emptyList())
                }
            }
        }

    private suspend fun queryPlans(): Map<Plan, ProductDetails> {
        val out = mutableMapOf<Plan, ProductDetails>()
        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            val list = withContext(Dispatchers.IO) { queryProductDetails(type) }
            list.forEach { details ->
                Plan.fromProductId(details.productId)?.let { out[it] = details }
            }
        }
        return out
    }

    private suspend fun queryPurchases(type: String): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            client.queryPurchasesAsync(params) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(list.orEmpty())
                } else {
                    cont.resume(emptyList())
                }
            }
        }

    private suspend fun queryAllPurchases(): List<Purchase> {
        val out = mutableListOf<Purchase>()
        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            out += withContext(Dispatchers.IO) { queryPurchases(type) }
        }
        return out
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val activeIds = mutableSetOf<String>()
        val activeTokens = mutableSetOf<String>()
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            activeTokens += purchase.purchaseToken
            purchase.products.forEach { id ->
                Plan.fromProductId(id)?.let { plan ->
                    activeIds += id
                    premium.grant(plan, purchase.purchaseToken)
                    if (!purchase.isAcknowledged) acknowledge(purchase)
                }
            }
        }
        premium.reconcile(activeIds, activeTokens)
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { _ -> }
    }

    private suspend fun restoreEntitlement() {
        val purchases = queryAllPurchases()
        handlePurchases(purchases)
    }
}
