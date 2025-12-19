package com.ambufast.dailyexpenses

data class Transaction(
    val category: String?,
    val description: String,
    val amount: String,
    val date: String,
    val isDeposit: Boolean,
    val monthYear: String
)


