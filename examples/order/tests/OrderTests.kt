package kotlinassay.examples.orders.tests

import kotlinassay.examples.orders.*
import kotlinx.collections.immutable.persistentListOf

fun main() {
    val line = DraftLine("book", 2, 1250)
    val checks = listOf(
        createOrder("", persistentListOf(line)) == Rejected("order identifier is required"),
        createOrder("order", persistentListOf()) == Rejected("an order needs at least one line"),
        createOrder("order", persistentListOf(line.copy(sku = ""))) == Rejected("each line needs a SKU"),
        createOrder("order", persistentListOf(line.copy(quantity = 0))) == Rejected("quantity must be positive"),
        createOrder("order", persistentListOf(line.copy(unitPriceCents = -1))) == Rejected("unit price cannot be negative"),
        createOrder("order", persistentListOf(line)) == Accepted(Order("order", persistentListOf(line))),
        totalCents(Order("order", persistentListOf(line))) == 2500L,
        totalCents(Order("order", persistentListOf(line, line.copy(quantity = 1)))) == 3750L,
    )
    check(checks.all { it }) { "Order example regression" }
    println("Order example checks: ${checks.size}; failed: 0")
}
