package playground.expenseguard.tests
import playground.expenseguard.Approved
import playground.expenseguard.Decision
import playground.expenseguard.Invalid
import playground.expenseguard.NeedsReview
import playground.expenseguard.decide
import kotlin.system.exitProcess
data class Case(val id:String,val amount:Long,val currency:String,val memo:String,val limit:Long,val expected:Decision)
fun main(args:Array<String>){
 if(args.size!=1||args[0] !in listOf("baseline","expanded"))exitProcess(64)
 val cases=listOf(
        Case("approved", 2500L, "USD", "lunch", 10000L, Approved(2500L)),
        Case("negative", -1L, "USD", "lunch", 10000L, Invalid("amount")),
        Case("over-limit", 10001L, "USD", "lunch", 10000L, NeedsReview(10001L)),
        Case("currency", 2500L, "EUR", "lunch", 10000L, Invalid("currency")),
        Case("empty-memo", 2500L, "USD", "", 10000L, Invalid("memo")),
        Case("zero-policy", 2500L, "USD", "lunch", 0L, Invalid("policy")),
        Case("zero-amount", 0L, "USD", "lunch", 10000L, Invalid("amount")),
        Case("exact-limit", 10000L, "USD", "lunch", 10000L, Approved(10000L)),
        Case("smallest-amount", 1L, "USD", "lunch", 10000L, Approved(1L)),
        Case("blank-memo", 2500L, "USD", " \t", 10000L, Invalid("memo")),
        Case("negative-policy", 2500L, "USD", "lunch", -1L, Invalid("policy")),
        Case("large-amount", 2147483647L, "USD", "lunch", 10000L, NeedsReview(2147483647L)),
        Case("memo-200-ascii", 2500L, "USD", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", 10000L, Approved(2500L)),
        Case("memo-201-ascii", 2500L, "USD", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", 10000L, Invalid("memo-too-long")),
        Case("memo-200-astral", 2500L, "USD", "😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀", 10000L, Approved(2500L)),
        Case("memo-201-astral", 2500L, "USD", "😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀", 10000L, Invalid("memo-too-long")),
        Case("memo-200-combining", 2500L, "USD", "éééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééé", 10000L, Approved(2500L)),
        Case("memo-202-combining", 2500L, "USD", "ééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééééé", 10000L, Invalid("memo-too-long")),
        Case("long-blank-precedence", 2500L, "USD", "                                                                                                                                                                                                         ", 10000L, Invalid("memo")),
        Case("bad-policy-precedence", 2500L, "USD", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", 0L, Invalid("policy")),
        Case("bad-currency-precedence", 2500L, "EUR", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", 10000L, Invalid("currency")),
        Case("bad-amount-precedence", -1L, "USD", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", 10000L, Invalid("amount")),
 )
 val selected=if(args[0]=="baseline")cases.take(12)else cases
 val failures=selected.count{case->
  val passed=decide(case.amount,case.currency,case.memo,case.limit)==case.expected
  println("CASE\t${case.id}\t${if(passed)"PASS" else "FAIL"}")
  !passed
 }
 exitProcess(if(failures==0)0 else 1)
}
