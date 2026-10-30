package playground.expenseguard

sealed interface Decision
data class Approved(val amount: Long) : Decision
data class NeedsReview(val amount: Long) : Decision
data class Invalid(val reason: String) : Decision

fun decide(amount: Long, currency: String, memo: String, limit: Long): Decision = when {
    limit <= 0 -> Invalid("policy")
    amount <= 0 -> Invalid("amount")
    currency != "USD" -> Invalid("currency")
    memo.isBlank() -> Invalid("memo")
    amount > limit -> NeedsReview(amount)
    else -> Approved(amount)
}
