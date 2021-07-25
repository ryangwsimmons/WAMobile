package io.github.ryangwsimmons.wamobile

data class AccountViewTerm(val id: String, val name: String, val balance: Double, val categories: ArrayList<AccountViewCategory> = ArrayList<AccountViewCategory>())

data class AccountViewCategory(val name: String, val amount: Double, val transactions: ArrayList<AccountViewTransaction> = ArrayList<AccountViewTransaction>())

data class AccountViewTransaction(val name: String, val amount: Double, val chargeDetails: ArrayList<AccountViewChargeDetails> = ArrayList<AccountViewChargeDetails>())

data class AccountViewChargeDetails(val name: String, val amount: Double)