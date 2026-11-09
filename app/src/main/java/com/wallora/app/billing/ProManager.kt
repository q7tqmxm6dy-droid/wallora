package com.wallora.app.billing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.util.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the paywall needs to render. */
data class ProState(
    /** Entitlement. Starts from the cached value, then follows Google Play. */
    val isPro: Boolean = false,
    /** Localised price such as "$4.99", or null until Play has answered. */
    val price: String? = null,
    /** A purchase is waiting on payment (cash, bank transfer, ...). */
    val pending: Boolean = false,
    /** Play answered but billing or the product is missing (no Play Store, product not active yet). */
    val unavailable: Boolean = false,
    /** One-shot message for the paywall: a [ProMessage], cleared by [ProManager.consumeMessage]. */
    val message: ProMessage? = null,
)

enum class ProMessage { PURCHASED, RESTORED, NOTHING_TO_RESTORE, PENDING, UNAVAILABLE, FAILED }

/**
 * Wallora Pro: a single non-consumable in-app product that removes ads and unlocks the Pro
 * features. Google Play is the source of truth; the result is cached in DataStore so the app
 * behaves correctly offline and from a cold start.
 *
 * Create the product in Play Console (Monetize > Products > In-app products) with the id
 * [PRODUCT_ID] and activate it. Until then [ProState.price] stays null and the paywall shows the
 * button disabled instead of failing.
 */
@SuppressLint("StaticFieldLeak") // only ever holds the application context
object ProManager {

    const val PRODUCT_ID = "wallora_pro"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(ProState())
    val state: StateFlow<ProState> = _state.asStateFlow()

    private lateinit var appContext: Context
    private lateinit var prefs: PreferencesRepository
    private var client: BillingClient? = null
    private var details: ProductDetails? = null
    private var started = false

    private val purchaseListener = PurchasesUpdatedListener { result, purchases ->
        scope.launch {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> handle(purchases.orEmpty(), fromPurchaseFlow = true)
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh(restoring = true)
                BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                else -> {
                    Analytics.log(appContext, "pro_purchase_error", mapOf("code" to result.responseCode))
                    message(ProMessage.FAILED)
                }
            }
        }
    }

    /** Call once from `Application.onCreate`. */
    fun start(context: Context) {
        if (started) return
        started = true
        appContext = context.applicationContext
        prefs = PreferencesRepository(appContext)

        scope.launch {
            // Show the cached entitlement immediately, before Play answers.
            val cached = prefs.isProNow()
            _state.update { it.copy(isPro = cached) }
            connect()
        }
    }

    private fun connect() {
        val billing = BillingClient.newBuilder(appContext)
            .setListener(purchaseListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = billing
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        loadDetails()
                        refresh()
                    }
                } else {
                    _state.update { it.copy(unavailable = true) }
                }
            }

            override fun onBillingServiceDisconnected() = Unit // auto-reconnect is on
        })
    }

    private suspend fun loadDetails() {
        val billing = client?.takeIf { it.isReady } ?: return
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billing.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        )
        val found = result.productDetailsList?.firstOrNull()
        details = found
        val price = found?.oneTimePurchaseOfferDetails?.formattedPrice
        _state.update { it.copy(price = price, unavailable = price == null) }
    }

    /** Re-reads what the user owns. Also the "restore purchases" action. */
    fun restore() {
        scope.launch { refresh(restoring = true) }
    }

    private suspend fun refresh(restoring: Boolean = false) {
        val billing = client?.takeIf { it.isReady } ?: run {
            if (restoring) message(ProMessage.UNAVAILABLE)
            return
        }
        val result = billing.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            // Keep the cached value rather than revoking Pro because of a transient error.
            if (restoring) message(ProMessage.UNAVAILABLE)
            return
        }
        handle(result.purchasesList, fromPurchaseFlow = false, restoring = restoring)
    }

    private suspend fun handle(
        purchases: List<Purchase>,
        fromPurchaseFlow: Boolean,
        restoring: Boolean = false,
    ) {
        val mine = purchases.filter { PRODUCT_ID in it.products }
        val owned = mine.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = mine.any { it.purchaseState == Purchase.PurchaseState.PENDING }

        if (owned != null && !owned.isAcknowledged) {
            // Unacknowledged purchases are refunded by Google after three days.
            client?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(owned.purchaseToken).build(),
            )
        }

        val isPro = owned != null
        prefs.setPro(isPro)
        _state.update { it.copy(isPro = isPro, pending = pending) }

        when {
            isPro && fromPurchaseFlow -> {
                Analytics.log(appContext, "pro_purchased")
                message(ProMessage.PURCHASED)
            }
            isPro && restoring -> message(ProMessage.RESTORED)
            pending -> message(ProMessage.PENDING)
            restoring && !isPro -> message(ProMessage.NOTHING_TO_RESTORE)
        }
    }

    fun purchase(activity: Activity) {
        val billing = client?.takeIf { it.isReady }
        val product = details
        if (billing == null || product == null) {
            message(ProMessage.UNAVAILABLE)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
            )
            .build()
        val result = billing.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) message(ProMessage.FAILED)
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun message(value: ProMessage) = _state.update { it.copy(message = value) }
}
