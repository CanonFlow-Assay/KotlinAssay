package kotlinassay.examples.orders

import kotlinx.collections.immutable.PersistentList

data class DraftLine(val sku: String, val quantity: Int, val unitPriceCents: Long)
data class Order(val identifier: String, val lines: PersistentList<DraftLine>)
sealed interface OrderDecision
data class Accepted(val order: Order): OrderDecision
data class Rejected(val reason: String): OrderDecision

fun createOrder(identifier: String, lines: PersistentList<DraftLine>): OrderDecision = when {
    identifier.isBlank() -> Rejected("order identifier is required")
    lines.isEmpty() -> Rejected("an order needs at least one line")
    lines.any { it.sku.isBlank() } -> Rejected("each line needs a SKU")
    lines.any { it.quantity <= 0 } -> Rejected("quantity must be positive")
    lines.any { it.unitPriceCents < 0 } -> Rejected("unit price cannot be negative")
    else -> Accepted(Order(identifier, lines))
}

// Illustrative arithmetic only; callers must bound values to prevent Long overflow.
fun totalCents(order: Order): Long = order.lines.fold(0L) { total, line ->
    total + line.quantity * line.unitPriceCents
}
