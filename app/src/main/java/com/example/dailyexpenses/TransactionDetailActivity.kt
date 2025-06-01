package com.example.dailyexpenses

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TransactionDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        val category = intent.getStringExtra("category")
        val amount = intent.getStringExtra("amount")
        val description = intent.getStringExtra("description")
        val date = intent.getStringExtra("date")
        val monthYear = intent.getStringExtra("monthYear")

        findViewById<TextView>(R.id.detailCategory).text = "Category: $category"
        findViewById<TextView>(R.id.detailAmount).text = "Amount: ৳$amount"
        findViewById<TextView>(R.id.detailDescription).text = "Description: $description"
        findViewById<TextView>(R.id.detailDate).text = "Date: $date"
        findViewById<TextView>(R.id.detailMonthYear).text = "Month: $monthYear"
    }
}
