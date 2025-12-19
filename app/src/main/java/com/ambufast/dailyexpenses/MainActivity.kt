package com.ambufast.dailyexpenses

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {
    private lateinit var transactionList: MutableList<Transaction>
    private lateinit var adapter: TransactionAdapter
    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowInsetsControllerCompat(window, window.decorView)
            .hide(WindowInsetsCompat.Type.statusBars())
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        dbHelper = DBHelper(this)
        transactionList = dbHelper.getAllTransactions().toMutableList()
        adapter = TransactionAdapter(transactionList)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        val monthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
        val existingDeposit = dbHelper.getDepositForMonth(monthYear)
        Log.d("deposit", existingDeposit.toString())
        val totalBalanceText = if (existingDeposit != null) {
            "৳ $existingDeposit"
        } else {
            "৳ 0"
        }
        findViewById<TextView>(R.id.totalBlnc).text = totalBalanceText
        val totalExpenses = dbHelper.getTotalExpensesForMonth(monthYear)
        findViewById<TextView>(R.id.totalExp).text = "৳ $totalExpenses"
        Log.d("expenses", totalExpenses)
        findViewById<TextView>(R.id.curMonth).text = "Current Month:  $monthYear"

        val fab = findViewById<FloatingActionButton>(R.id.fabAddTransaction)
        fab.setOnClickListener {
            showAddDialog()
        }
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            showResetConfirmationDialog()
        }

    }


    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.add_transaction_dialog, null)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val editDescription = dialogView.findViewById<EditText>(R.id.editDescription)
        val editAmount = dialogView.findViewById<EditText>(R.id.editAmount)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioType)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddTransaction)

        val categories = listOf("Food", "Transport", "Bills", "Shopping", "Salary", "Other")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapterSpinner

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        spinnerCategory.visibility = View.GONE
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            spinnerCategory.visibility = if (checkedId == R.id.radioDeposit) View.GONE else View.VISIBLE
        }

        btnAdd.setOnClickListener {
            val isDeposit = radioGroup.checkedRadioButtonId == R.id.radioDeposit
            val amount = editAmount.text.toString().trim()
            val description = editDescription.text.toString().trim()
            val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
            val monthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())

            if (amount.isEmpty() || (!isDeposit && description.isEmpty())) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isDeposit) {
                val existingDeposit = dbHelper.getDepositForMonth(monthYear)
                val depositTransaction = Transaction(
                    category = "Deposit",
                    description = description.ifEmpty { "Monthly deposit" },
                    amount = amount,
                    date = date,
                    isDeposit = true,
                    monthYear = monthYear
                )

                if (existingDeposit != null) {
                    dbHelper.updateDeposit(monthYear, amount)

                } else {
                    dbHelper.insertTransaction(depositTransaction)

                }
                val totalDepo = dbHelper.getDepositForMonth(monthYear)
                val totalBalanceText = if (totalDepo != null) {
                    "৳ $totalDepo"
                } else {
                    "৳ 0"
                }
                findViewById<TextView>(R.id.totalBlnc).text = totalBalanceText

                transactionList.add(0, depositTransaction)
                adapter.notifyItemInserted(0)
                dialog.dismiss()
            } else {
                val expenseAmount = amount.toDoubleOrNull() ?: 0.0

                // 🔐 Check if this expense would exceed total balance
                val currentTotalExpenses = dbHelper.getTotalExpensesForMonth(monthYear).toDoubleOrNull() ?: 0.0
                val currentDeposit = dbHelper.getDepositForMonth(monthYear)?.toDoubleOrNull() ?: 0.0
Log.d("Total Expenses" +
        "                                                            ", currentTotalExpenses.toString())
Log.d("deposit", currentDeposit.toString())
                if (currentTotalExpenses + expenseAmount > currentDeposit) {
                    Toast.makeText(this, "Cannot add this expense. Monthly balance exceeded.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Proceed with inserting the expense
                val category = spinnerCategory.selectedItem.toString()
                val expenseTransaction = Transaction(
                    category = category,
                    description = description,
                    amount = amount,
                    date = date,
                    isDeposit = false,
                    monthYear = monthYear
                )

                dbHelper.insertTransaction(expenseTransaction)
                val updatedTotalExpenses = dbHelper.getTotalExpensesForMonth(monthYear)
                findViewById<TextView>(R.id.totalExp).text = "৳ $updatedTotalExpenses"

                // 🔔 Check daily target and show notification
                val todayTotal = dbHelper.getTotalExpensesForDate(date) // Ensure this function exists
                val calendar = Calendar.getInstance()
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dailyTarget = if (daysInMonth > 0) currentDeposit / daysInMonth else 0.0

                if (todayTotal > dailyTarget) {
                    val builder = NotificationCompat.Builder(this, "expense_channel")
                        .setSmallIcon(R.drawable.notify) // Ensure this icon exists
                        .setContentTitle("Daily Limit Exceeded")
                        .setContentText("Today's expenses have exceeded your daily target!")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)

                    NotificationManagerCompat.from(this).notify(1, builder.build())
                }

                transactionList.add(0, expenseTransaction)
                adapter.notifyItemInserted(0)
                dialog.dismiss()
            }
        }



        dialog.show()
    }
    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset All Data")
            .setMessage("Are you sure you want to delete all transactions and reset your balance?")
            .setPositiveButton("Yes") { dialog, _ ->
                dbHelper.resetAllData()

                // Clear local transaction list and update UI
                transactionList.clear()
                adapter.notifyDataSetChanged()

                findViewById<TextView>(R.id.totalExp).text = "৳ 0"
                findViewById<TextView>(R.id.totalBlnc).text = "৳ 0"

                Toast.makeText(this, "All data has been reset.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Expense Alerts"
            val descriptionText = "Notifies when expenses exceed daily target"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("expense_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
