package com.skyrox.vpnapp.subscription

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import java.util.*
import java.util.concurrent.TimeUnit

class SubscriptionManager(context: Context) {
    private var billingClient: BillingClient
    private val prefs = context.getSharedPreferences("SubscriptionPrefs", Context.MODE_PRIVATE)

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("SubscriptionManager", "Google Play Billing подключен")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e("SubscriptionManager", "Биллинг отключен")
            }
        })
    }

    // Запуск покупки подписки
    fun purchaseSubscription(activity: Activity, sku: String) {
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(sku)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                ).build()
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0] // Получаем детали подписки

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    ).build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                Log.e("SubscriptionManager", "Ошибка загрузки подписки: ${billingResult.responseCode}")
            }
        }
    }


    // Обрабатываем покупку
    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                saveSubscription(purchase.purchaseTime)
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d("SubscriptionManager", "Подписка подтверждена")
                        }
                    }
                }
            }
        }
    }

    // Сохраняем дату окончания подписки
    private fun saveSubscription(purchaseTime: Long) {
        val endDate = Calendar.getInstance().apply {
            timeInMillis = purchaseTime
            add(Calendar.MONTH, 1) // Укажите корректный период подписки
        }.timeInMillis

        prefs.edit().putLong("subscription_end_date", endDate).apply()
    }

    // Проверяем, действует ли подписка
    fun isSubscriptionActive(): Boolean {
        val endDate = prefs.getLong("subscription_end_date", 0)
        return System.currentTimeMillis() < endDate
    }

    // Получаем оставшиеся дни подписки
    fun getRemainingDays(callback: (Int) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activeSubscription = purchases.find { it.products.contains("vpn_premium_monthly") }
                if (activeSubscription != null) {
                    val subscriptionEndTime = activeSubscription.purchaseTime + TimeUnit.DAYS.toMillis(30)
                    val remainingTime = subscriptionEndTime - System.currentTimeMillis()
                    callback((remainingTime / (1000 * 60 * 60 * 24)).toInt())
                } else {
                    callback(0)
                }
            } else {
                callback(0)
            }
        }
    }

    // Проверяем подписку при запуске приложения
    fun isSubscriptionActive(callback: (Boolean) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activeSubscription = purchases.find { it.products.contains("vpn_premium_monthly") }
                if (activeSubscription != null) {
                    val subscriptionEndTime = activeSubscription.purchaseTime + TimeUnit.DAYS.toMillis(30) // Подписка на месяц
                    prefs.edit().putLong("subscription_end_date", subscriptionEndTime).apply()
                    callback(System.currentTimeMillis() < subscriptionEndTime)
                } else {
                    callback(false)
                }
            } else {
                callback(false)
            }
        }
    }
}
