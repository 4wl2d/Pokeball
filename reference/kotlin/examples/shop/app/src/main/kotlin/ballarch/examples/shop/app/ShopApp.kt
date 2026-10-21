package ballarch.examples.shop.app

import ballarch.examples.shop.checkout.CheckoutFlow
import ballarch.examples.shop.inventory.InventoryBall
import ballarch.examples.shop.orders.OrderBall
import ballarch.examples.shop.payments.api.PaymentResolved
import ballarch.runtime.Composition

/** Composition root of the whole shop (slice S3). */
fun shopComposition(provider: ProviderAdapters, initialStock: Int): Composition =
    paymentsComposition(provider)
        .ball(CheckoutFlow())
        .ball(InventoryBall(initialStock))
        .ball(OrderBall)
        .subscribe(PaymentResolved, "checkout") { it.orderId }
