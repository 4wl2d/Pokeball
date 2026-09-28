package pokeball.examples.shop.app

import pokeball.examples.shop.checkout.CheckoutFlow
import pokeball.examples.shop.inventory.InventoryBall
import pokeball.examples.shop.orders.OrderBall
import pokeball.examples.shop.payments.api.PaymentResolved
import pokeball.runtime.Composition

/** Composition root of the whole shop (slice S3). */
fun shopComposition(provider: ProviderAdapters, initialStock: Int): Composition =
    paymentsComposition(provider)
        .ball(CheckoutFlow())
        .ball(InventoryBall(initialStock))
        .ball(OrderBall)
        .subscribe(PaymentResolved, "checkout") { it.orderId }
