package com.ambufast.dailyexpenses

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
        val textAmount: TextView = itemView.findViewById(R.id.textAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.textTitle.text = transaction.category
        holder.textDate.text = transaction.date

        val sign = if (transaction.isDeposit) "+" else "-"
        holder.textAmount.text = "$sign${transaction.amount}"

        val color = if (transaction.isDeposit)
            ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
        else
            ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)

        holder.textAmount.setTextColor(color)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TransactionDetailActivity::class.java).apply {
                putExtra("category", transaction.category)
                putExtra("amount", transaction.amount)
                putExtra("description", transaction.description)
                putExtra("date", transaction.date)
                putExtra("monthYear", transaction.monthYear)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = transactions.size
}

