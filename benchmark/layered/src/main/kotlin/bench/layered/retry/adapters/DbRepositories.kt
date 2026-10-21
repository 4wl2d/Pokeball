package bench.layered.retry.adapters

import bench.env.SimDb
import bench.layered.retry.application.InventoryRepository
import bench.layered.retry.application.OrderRepository
import bench.layered.retry.domain.CheckoutOrder

class DbOrderRepository(private val db: SimDb) : OrderRepository {
    override fun find(id: String): CheckoutOrder? = db.get("order/$id") as CheckoutOrder?

    override fun save(order: CheckoutOrder) = db.transaction { it.put("order/${order.id}", order) }

    override fun unfinished(): List<CheckoutOrder> =
        db.keys("order/").map { db.get(it) as CheckoutOrder }.filter { !it.isTerminal }
}

class DbInventoryRepository(private val db: SimDb, private val initialStock: Map<String, Int>) : InventoryRepository {
    override fun reserve(orderId: String, sku: String, quantity: Int): Boolean = db.transaction { tx ->
        if (tx.get<Int>("reservation/$orderId") != null) return@transaction true
        val stock = tx.get<Int>("stock/$sku") ?: initialStock[sku] ?: 0
        if (stock < quantity) return@transaction false
        tx.put("stock/$sku", stock - quantity)
        tx.put("reservation/$orderId", quantity)
        tx.put("reservation-sku/$orderId", sku)
        true
    }

    override fun release(orderId: String) = db.transaction { tx ->
        val quantity = tx.get<Int>("reservation/$orderId") ?: return@transaction
        val sku = tx.get<String>("reservation-sku/$orderId")!!
        tx.put("stock/$sku", (tx.get<Int>("stock/$sku") ?: 0) + quantity)
        tx.delete("reservation/$orderId")
        tx.delete("reservation-sku/$orderId")
    }
}
