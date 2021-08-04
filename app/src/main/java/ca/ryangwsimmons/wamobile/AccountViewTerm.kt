package ca.ryangwsimmons.wamobile

data class AccountViewTerm(val id: String, val name: String, val balance: Double, val categories: ArrayList<AccountViewCategory> = ArrayList())

data class AccountViewCategory(val name: String, val amount: Double, val transactions: ArrayList<AccountViewTransaction> = ArrayList())

data class AccountViewTransaction(val name: String, val amount: Double, val chargeDetails: ArrayList<AccountViewChargeDetails> = ArrayList())

data class AccountViewChargeDetails(val name: String, val amount: Double)