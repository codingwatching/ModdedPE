package com.mcal.mcpelauncher.iap

interface PurchaseServiceListener : BillingServiceListener {
    /**
     * Callback will be triggered upon obtaining information about product prices
     *
     * @param iapKeyPrices - a map with available products
     */
    override fun onPricesUpdated(iapKeyPrices: Map<String, String>)

    /**
     * Callback will be triggered when a product purchased successfully
     *
     * @param sku - specificator of owned product
     */
    fun onProductPurchased(sku: String?)

    /**
     * Callback will be triggered upon owned products restore
     *
     * @param sku - specificator of owned product
     */
    fun onProductRestored(sku: String?)
}